package com.backend.aurum.domain.mcp.controller;

import com.backend.aurum.domain.analytics.dto.AnalyticsSummaryDTO;
import com.backend.aurum.domain.analytics.dto.TargetDTO;
import com.backend.aurum.domain.analytics.mapper.TargetMapper;
import com.backend.aurum.domain.analytics.model.Target;
import com.backend.aurum.domain.analytics.model.TargetType;
import com.backend.aurum.domain.analytics.service.AnalyticsService;
import com.backend.aurum.domain.analytics.service.TargetService;
import com.backend.aurum.domain.asset.dto.AssetCategoryDTO;
import com.backend.aurum.domain.asset.dto.AssetDTO;
import com.backend.aurum.domain.asset.dto.SnapshotDTO;
import com.backend.aurum.domain.asset.mapper.AssetCategoryMapper;
import com.backend.aurum.domain.asset.mapper.AssetMapper;
import com.backend.aurum.domain.asset.mapper.SnapshotMapper;
import com.backend.aurum.domain.asset.model.Asset;
import com.backend.aurum.domain.asset.model.AssetCategory;
import com.backend.aurum.domain.asset.model.AssetType;
import com.backend.aurum.domain.asset.model.Snapshot;
import com.backend.aurum.domain.asset.service.AssetCategoryService;
import com.backend.aurum.domain.asset.service.AssetService;
import com.backend.aurum.domain.asset.service.SnapshotService;
import com.backend.aurum.domain.user.enums.Currency;
import com.backend.aurum.domain.user.model.User;
import com.backend.aurum.infrastructure.exchange.ExchangeRateService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/mcp")
@RequiredArgsConstructor
public class McpController {

	private final AssetService assetService;
	private final AssetMapper assetMapper;
	private final SnapshotService snapshotService;
	private final SnapshotMapper snapshotMapper;
	private final TargetService targetService;
	private final TargetMapper targetMapper;
	private final AnalyticsService analyticsService;
	private final ExchangeRateService exchangeRateService;
	private final AssetCategoryService categoryService;
	private final AssetCategoryMapper categoryMapper;
	private final ObjectMapper objectMapper;

	private record McpSession(SseEmitter emitter, User user) {}

	private final Map<String, McpSession> sessions = new ConcurrentHashMap<>();

	@GetMapping(value = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public SseEmitter connect() {
		User user = currentUser();
		String sessionId = UUID.randomUUID().toString();
		SseEmitter emitter = new SseEmitter(300_000L);

		sessions.put(sessionId, new McpSession(emitter, user));
		emitter.onCompletion(() -> sessions.remove(sessionId));
		emitter.onTimeout(() -> sessions.remove(sessionId));
		emitter.onError(e -> sessions.remove(sessionId));

		try {
			emitter.send(SseEmitter.event().name("endpoint").data("/mcp/message?sessionId=" + sessionId));
		} catch (IOException e) {
			emitter.completeWithError(e);
		}

		return emitter;
	}

	@PostMapping("/message")
	public ResponseEntity<Void> handleMessage(
		@RequestParam String sessionId,
		@RequestBody String body
	) {
		McpSession session = sessions.get(sessionId);
		if (session == null) {
			return ResponseEntity.badRequest().build();
		}

		Thread.ofVirtual().start(() -> {
			try {
				processMessage(session, body);
			} catch (Exception ignored) {}
		});

		return ResponseEntity.ok().build();
	}

	private void processMessage(McpSession session, String body) throws Exception {
		JsonNode request = objectMapper.readTree(body);
		String method = request.path("method").asText();
		JsonNode id = request.path("id");

		if (id.isMissingNode() || id.isNull()) {
			return;
		}

		JsonNode result;
		try {
			result = switch (method) {
				case "initialize" -> handleInitialize();
				case "ping" -> objectMapper.createObjectNode();
				case "tools/list" -> handleToolsList();
				case "tools/call" -> handleToolsCall(request, session.user());
				default -> throw new IllegalArgumentException("Method not found: " + method);
			};
		} catch (IllegalArgumentException e) {
			sendError(session.emitter(), id, -32601, e.getMessage());
			return;
		} catch (Exception e) {
			sendError(session.emitter(), id, -32603, "Internal error: " + e.getMessage());
			return;
		}

		ObjectNode response = objectMapper.createObjectNode();
		response.put("jsonrpc", "2.0");
		response.set("id", id);
		response.set("result", result);

		session
			.emitter()
			.send(SseEmitter.event().name("message").data(objectMapper.writeValueAsString(response)));
	}

	private void sendError(SseEmitter emitter, JsonNode id, int code, String message)
		throws Exception {
		ObjectNode errorObj = objectMapper.createObjectNode();
		errorObj.put("code", code);
		errorObj.put("message", message);

		ObjectNode response = objectMapper.createObjectNode();
		response.put("jsonrpc", "2.0");
		response.set("id", id);
		response.set("error", errorObj);

		emitter.send(
			SseEmitter.event().name("message").data(objectMapper.writeValueAsString(response))
		);
	}

	private ObjectNode handleInitialize() {
		ObjectNode serverInfo = objectMapper.createObjectNode();
		serverInfo.put("name", "Aurum MCP Server");
		serverInfo.put("version", "1.0.0");

		ObjectNode capabilities = objectMapper.createObjectNode();
		capabilities.set("tools", objectMapper.createObjectNode());

		ObjectNode result = objectMapper.createObjectNode();
		result.put("protocolVersion", "2024-11-05");
		result.set("capabilities", capabilities);
		result.set("serverInfo", serverInfo);
		return result;
	}

	private ObjectNode handleToolsList() {
		ArrayNode tools = objectMapper.createArrayNode();
		tools.add(buildTool("get_assets", "Get all assets for the current user", buildSchema()));
		tools.add(
			buildTool(
				"get_snapshots",
				"Get all value snapshots for a specific asset",
				buildSchema("assetId", "string", "UUID of the asset", true)
			)
		);
		tools.add(
			buildTool(
				"get_kpis",
				"Get financial KPIs: net worth, gross assets, liabilities, allocations, variations",
				buildSchema()
			)
		);
		tools.add(
			buildTool(
				"get_targets",
				"Get all financial targets/goals for the current user",
				buildSchema()
			)
		);
		tools.add(buildTool("add_asset", "Create a new financial asset", buildAddAssetSchema()));
		tools.add(
			buildTool(
				"add_snapshot",
				"Add a value snapshot to an existing asset",
				buildAddSnapshotSchema()
			)
		);
		tools.add(
			buildTool("add_target", "Create a new financial target/goal", buildAddTargetSchema())
		);
		tools.add(
			buildTool(
				"get_categories",
				"Get all asset categories (system-wide and user-defined)",
				buildSchema()
			)
		);
		tools.add(
			buildTool(
				"add_category",
				"Create a new user-defined asset category",
				buildAddCategorySchema()
			)
		);

		ObjectNode result = objectMapper.createObjectNode();
		result.set("tools", tools);
		return result;
	}

	private JsonNode handleToolsCall(JsonNode request, User user) throws Exception {
		JsonNode params = request.path("params");
		String name = params.path("name").asText();
		JsonNode args = params.path("arguments");

		String text;
		try {
			text = switch (name) {
				case "get_assets" -> getAssets(user);
				case "get_snapshots" -> getSnapshots(user, args);
				case "get_kpis" -> getKpis(user);
				case "get_targets" -> getTargets(user);
				case "add_asset" -> addAsset(user, args);
				case "add_snapshot" -> addSnapshot(user, args);
				case "add_target" -> addTarget(user, args);
				case "get_categories" -> getCategories(user);
				case "add_category" -> addCategory(user, args);
				default -> throw new IllegalArgumentException("Unknown tool: " + name);
			};
		} catch (Exception e) {
			ArrayNode errorContent = objectMapper.createArrayNode();
			ObjectNode errorItem = objectMapper.createObjectNode();
			errorItem.put("type", "text");
			errorItem.put("text", "Error: " + e.getMessage());
			errorContent.add(errorItem);
			ObjectNode errorResult = objectMapper.createObjectNode();
			errorResult.set("content", errorContent);
			errorResult.put("isError", true);
			return errorResult;
		}

		ArrayNode content = objectMapper.createArrayNode();
		ObjectNode item = objectMapper.createObjectNode();
		item.put("type", "text");
		item.put("text", text);
		content.add(item);

		ObjectNode result = objectMapper.createObjectNode();
		result.set("content", content);
		return result;
	}

	// --- Tool implementations ---

	private String getAssets(User user) throws Exception {
		List<AssetDTO> assets = assetService
			.findAll(user.getId())
			.stream()
			.map(assetMapper::toDto)
			.toList();
		return objectMapper.writeValueAsString(assets);
	}

	private String getSnapshots(User user, JsonNode args) throws Exception {
		UUID assetId = UUID.fromString(args.path("assetId").asText());
		// Ownership check
		assetService.findById(assetId, user.getId());
		List<SnapshotDTO> snapshots = snapshotService
			.findByAssetId(assetId)
			.stream()
			.map(snapshotMapper::toDto)
			.toList();
		return objectMapper.writeValueAsString(snapshots);
	}

	private String getKpis(User user) throws Exception {
		AnalyticsSummaryDTO summary = analyticsService.getSummary(user.getId());
		return objectMapper.writeValueAsString(summary);
	}

	private String getTargets(User user) throws Exception {
		BigDecimal currentNetWorth = analyticsService.getSummary(user.getId()).getTotalNetWorth();
		List<TargetDTO> targets = targetService
			.findAll(user.getId())
			.stream()
			.map(t -> targetMapper.toDto(t, currentNetWorth))
			.toList();
		return objectMapper.writeValueAsString(targets);
	}

	private String addAsset(User user, JsonNode args) throws Exception {
		AssetDTO dto = new AssetDTO();
		dto.setName(args.path("name").asText());

		if (args.hasNonNull("categoryId")) {
			dto.setCategoryId(UUID.fromString(args.path("categoryId").asText()));
		}
		if (args.hasNonNull("originalCurrency")) {
			dto.setOriginalCurrency(Currency.valueOf(args.path("originalCurrency").asText()));
		}
		dto.setIsActive(true);
		dto.setIsFavorite(false);

		BigDecimal initialValue = null;
		LocalDate referenceDate = null;
		if (args.hasNonNull("initialValue")) {
			initialValue = new BigDecimal(args.path("initialValue").asText());
		}
		if (args.hasNonNull("referenceDate")) {
			referenceDate = LocalDate.parse(args.path("referenceDate").asText());
		}

		Asset asset = assetMapper.toEntity(dto, user.getId());
		Asset saved = assetService.save(asset, initialValue, referenceDate);
		return objectMapper.writeValueAsString(assetMapper.toDto(saved));
	}

	private String addSnapshot(User user, JsonNode args) throws Exception {
		UUID assetId = UUID.fromString(args.path("assetId").asText());
		BigDecimal amount = new BigDecimal(args.path("amountOriginalCurrency").asText());
		LocalDate referenceDate = LocalDate.parse(args.path("referenceDate").asText());

		Asset asset = assetService.findById(assetId, user.getId());

		BigDecimal exchangeRate = BigDecimal.ONE;
		if (args.hasNonNull("exchangeRateToBase")) {
			exchangeRate = new BigDecimal(args.path("exchangeRateToBase").asText());
		} else {
			String assetCurrency = asset.getOriginalCurrency().getValue();
			String userCurrency = user.getCurrency().getValue();
			if (!assetCurrency.equals(userCurrency)) {
				exchangeRate = exchangeRateService.getRate(assetCurrency, userCurrency, referenceDate);
			}
		}

		Optional<Snapshot> existing = snapshotService.findExistingForMonth(assetId, referenceDate);
		Snapshot snapshot;
		if (existing.isPresent()) {
			snapshot = existing.get();
			snapshot.setAmountOriginalCurrency(amount);
			snapshot.setReferenceDate(referenceDate);
			snapshot.setExchangeRateToBase(exchangeRate);
		} else {
			snapshot = new Snapshot();
			snapshot.setAsset(asset);
			snapshot.setAmountOriginalCurrency(amount);
			snapshot.setReferenceDate(referenceDate);
			snapshot.setExchangeRateToBase(exchangeRate);
		}

		Snapshot saved = snapshotService.save(snapshot);
		return objectMapper.writeValueAsString(snapshotMapper.toDto(saved));
	}

	private String addTarget(User user, JsonNode args) throws Exception {
		TargetDTO dto = new TargetDTO();
		dto.setName(args.path("name").asText());
		dto.setTargetAmount(new BigDecimal(args.path("targetAmount").asText()));
		if (args.hasNonNull("currentAmount")) {
			dto.setCurrentAmount(new BigDecimal(args.path("currentAmount").asText()));
		}
		if (args.hasNonNull("deadline")) {
			dto.setDeadline(LocalDate.parse(args.path("deadline").asText()));
		}
		if (args.hasNonNull("type")) {
			dto.setType(TargetType.valueOf(args.path("type").asText()));
		}
		dto.setIsCompleted(false);

		Target target = targetMapper.toEntity(dto, user.getId());
		Target saved = targetService.save(target);
		BigDecimal netWorth = analyticsService.getSummary(user.getId()).getTotalNetWorth();
		return objectMapper.writeValueAsString(targetMapper.toDto(saved, netWorth));
	}

	private String getCategories(User user) throws Exception {
		List<AssetCategoryDTO> categories = categoryService
			.findAll(user.getId())
			.stream()
			.map(categoryMapper::toDto)
			.toList();
		return objectMapper.writeValueAsString(categories);
	}

	private String addCategory(User user, JsonNode args) throws Exception {
		AssetCategoryDTO dto = new AssetCategoryDTO();
		dto.setName(args.path("name").asText());
		dto.setType(AssetType.valueOf(args.path("type").asText()));
		AssetCategory category = categoryMapper.toEntity(dto, user.getId());
		AssetCategory saved = categoryService.save(category);
		return objectMapper.writeValueAsString(categoryMapper.toDto(saved));
	}

	// --- Schema helpers ---

	private ObjectNode buildTool(String name, String description, ObjectNode inputSchema) {
		ObjectNode tool = objectMapper.createObjectNode();
		tool.put("name", name);
		tool.put("description", description);
		tool.set("inputSchema", inputSchema);
		return tool;
	}

	private ObjectNode buildSchema(String... fieldNameTypDescRequired) {
		ObjectNode schema = objectMapper.createObjectNode();
		schema.put("type", "object");
		schema.set("properties", objectMapper.createObjectNode());
		schema.set("required", objectMapper.createArrayNode());
		return schema;
	}

	private ObjectNode buildSchema(
		String fieldName,
		String type,
		String description,
		boolean required
	) {
		ObjectNode schema = objectMapper.createObjectNode();
		schema.put("type", "object");
		ObjectNode props = objectMapper.createObjectNode();
		ObjectNode fieldDef = objectMapper.createObjectNode();
		fieldDef.put("type", type);
		fieldDef.put("description", description);
		props.set(fieldName, fieldDef);
		schema.set("properties", props);
		if (required) {
			ArrayNode req = objectMapper.createArrayNode();
			req.add(fieldName);
			schema.set("required", req);
		}
		return schema;
	}

	private ObjectNode buildAddAssetSchema() {
		ObjectNode schema = objectMapper.createObjectNode();
		schema.put("type", "object");
		ObjectNode props = objectMapper.createObjectNode();
		props.set("name", field("string", "Name of the asset"));
		props.set(
			"categoryId",
			field("string", "UUID of the asset category (use get_assets to discover existing categories)")
		);
		props.set("originalCurrency", field("string", "ISO currency code e.g. EUR, USD"));
		props.set("initialValue", field("number", "Initial value in the asset's original currency"));
		props.set("referenceDate", field("string", "Reference date in ISO format YYYY-MM-DD"));
		schema.set("properties", props);
		ArrayNode required = objectMapper.createArrayNode();
		required.add("name");
		schema.set("required", required);
		return schema;
	}

	private ObjectNode buildAddSnapshotSchema() {
		ObjectNode schema = objectMapper.createObjectNode();
		schema.put("type", "object");
		ObjectNode props = objectMapper.createObjectNode();
		props.set("assetId", field("string", "UUID of the asset"));
		props.set("amountOriginalCurrency", field("number", "Value in the asset's original currency"));
		props.set("referenceDate", field("string", "Reference date in ISO format YYYY-MM-DD"));
		props.set(
			"exchangeRateToBase",
			field("number", "Exchange rate to user's base currency (auto-fetched if omitted)")
		);
		schema.set("properties", props);
		ArrayNode required = objectMapper.createArrayNode();
		required.add("assetId");
		required.add("amountOriginalCurrency");
		required.add("referenceDate");
		schema.set("required", required);
		return schema;
	}

	private ObjectNode buildAddCategorySchema() {
		ObjectNode schema = objectMapper.createObjectNode();
		schema.put("type", "object");
		ObjectNode props = objectMapper.createObjectNode();
		props.set("name", field("string", "Name of the category"));
		props.set("type", field("string", "Category type: ASSET or LIABILITY"));
		schema.set("properties", props);
		ArrayNode required = objectMapper.createArrayNode();
		required.add("name");
		required.add("type");
		schema.set("required", required);
		return schema;
	}

	private ObjectNode buildAddTargetSchema() {
		ObjectNode schema = objectMapper.createObjectNode();
		schema.put("type", "object");
		ObjectNode props = objectMapper.createObjectNode();
		props.set("name", field("string", "Name of the target"));
		props.set("targetAmount", field("number", "Target amount in base currency"));
		props.set("currentAmount", field("number", "Current progress amount (for MANUAL type)"));
		props.set("deadline", field("string", "Deadline in ISO format YYYY-MM-DD"));
		props.set("type", field("string", "Target type: MANUAL or NET_WORTH"));
		schema.set("properties", props);
		ArrayNode required = objectMapper.createArrayNode();
		required.add("name");
		required.add("targetAmount");
		schema.set("required", required);
		return schema;
	}

	private ObjectNode field(String type, String description) {
		ObjectNode f = objectMapper.createObjectNode();
		f.put("type", type);
		f.put("description", description);
		return f;
	}

	private User currentUser() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		return (User) auth.getPrincipal();
	}
}
