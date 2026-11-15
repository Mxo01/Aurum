package com.backend.aurum.domain.user.service;

import com.auth0.client.mgmt.ManagementAPI;
import com.auth0.exception.Auth0Exception;
import com.backend.aurum.domain.analytics.repository.TargetRepository;
import com.backend.aurum.domain.asset.repository.AssetCategoryRepository;
import com.backend.aurum.domain.asset.repository.AssetRepository;
import com.backend.aurum.domain.auth.service.Auth0ManagementService;
import com.backend.aurum.domain.cashflow.repository.CashFlowRepository;
import com.backend.aurum.domain.shared.exception.NotFoundException;
import com.backend.aurum.domain.shared.exception.ValidationException;
import com.backend.aurum.domain.shared.logging.LogSafe;
import com.backend.aurum.domain.user.dto.UpdateCurrencyDTO;
import com.backend.aurum.domain.user.dto.UpdateLocaleDTO;
import com.backend.aurum.domain.user.dto.UpdateNameDTO;
import com.backend.aurum.domain.user.dto.UserExportDTO;
import com.backend.aurum.domain.user.enums.Currency;
import com.backend.aurum.domain.user.enums.Locale;
import com.backend.aurum.domain.user.model.User;
import com.backend.aurum.domain.user.model.UserPrincipal;
import com.backend.aurum.domain.user.repository.UserRepository;
import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Iterator;
import java.util.Objects;
import java.util.UUID;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

	private final UserRepository userRepository;
	private final Auth0ManagementService auth0ManagementService;
	private final AssetRepository assetRepository;
	private final AssetCategoryRepository categoryRepository;
	private final TargetRepository targetRepository;
	private final CashFlowRepository cashFlowRepository;

	public User findOrCreate(String jwtId) {
		log.debug("UserService#findOrCreate - Looking up user by jwtId={}", jwtId);
		return userRepository
			.findByJwtId(jwtId)
			.orElseGet(() -> {
				log.info("UserService#findOrCreate - No existing user found, creating a new user");
				try {
					User newUser = new User();
					newUser.setJwtId(jwtId);
					newUser.setCurrency(Currency.EUR);
					newUser.setLocale(Locale.EN_US);
					User created = userRepository.saveAndFlush(newUser);
					log.info("UserService#findOrCreate - New user created: userId={}", created.getId());
					return created;
				} catch (DataIntegrityViolationException e) {
					log.warn("UserService#findOrCreate - Concurrent insert detected, retrying the lookup");
					log.debug("UserService#findOrCreate - Concurrent insert for jwtId={}", jwtId);
					return userRepository
						.findByJwtId(jwtId)
						.orElseThrow(() -> new RuntimeException("Failed to find or create user", e));
				}
			});
	}

	@Transactional
	public User getUserById(UUID userId) {
		log.debug("UserService#getUserById - Fetching user: userId={}", userId);
		return userRepository
			.findById(Objects.requireNonNull(userId))
			.orElseThrow(() -> new NotFoundException("User not found"));
	}

	@Transactional
	public void deleteUser(UUID userId, String jwtId) {
		log.info("UserService#deleteUser - Deleting user: userId={}", userId);
		log.debug("UserService#deleteUser - Auth0 subject={}", jwtId);
		userRepository.deleteById(Objects.requireNonNull(userId));
		log.debug(
			"UserService#deleteUser - Local user record deleted, proceeding to delete Auth0 user"
		);

		try {
			ManagementAPI managementApi = auth0ManagementService.getManagementAPI();
			managementApi.users().delete(jwtId).execute();
			log.info("UserService#deleteUser - Auth0 user deleted successfully: userId={}", userId);
		} catch (Auth0Exception e) {
			log.error("UserService#deleteUser - Failed to delete the Auth0 user: userId={}", userId, e);
			throw new RuntimeException("Error during Auth0 user deletion", e);
		}
	}

	@Transactional
	public void updateAuth0Name(String jwtId, UpdateNameDTO dto) {
		log.debug("UserService#updateAuth0Name - Updating name for jwtId={}", jwtId);
		if (dto.name() == null || dto.name().isBlank()) {
			throw new ValidationException("name", "Name is required");
		}
		if (dto.name().length() > 100) {
			throw new ValidationException("name", "Name cannot exceed 100 characters");
		}
		try {
			ManagementAPI mgmt = auth0ManagementService.getManagementAPI();
			com.auth0.json.mgmt.users.User auth0User = new com.auth0.json.mgmt.users.User();
			auth0User.setName(dto.name());
			mgmt.users().update(jwtId, auth0User).execute();
			log.info("UserService#updateAuth0Name - Name updated in Auth0");
		} catch (Auth0Exception e) {
			log.error("UserService#updateAuth0Name - Failed to update the name in Auth0", e);
			throw new RuntimeException("Error during Auth0 name update: " + e.getMessage());
		}
	}

	@Transactional
	public void updateCurrency(UUID userId, UpdateCurrencyDTO dto) {
		log.debug(
			"UserService#updateCurrency - Updating currency for userId={}, newCurrency={}",
			userId,
			dto.currency()
		);
		if (dto.currency() == null) {
			throw new ValidationException("currency", "Select a currency");
		}
		User user = userRepository
			.findById(Objects.requireNonNull(userId))
			.orElseThrow(() -> new NotFoundException("User not found"));
		user.setCurrency(dto.currency());
		userRepository.save(user);
		log.info(
			"UserService#updateCurrency - Currency updated: userId={}, currency={}",
			userId,
			dto.currency()
		);
	}

	@Transactional
	public void updateLocale(UUID userId, UpdateLocaleDTO dto) {
		log.debug(
			"UserService#updateLocale - Updating locale for userId={}, newLocale={}",
			userId,
			dto.locale()
		);
		if (dto.locale() == null) {
			throw new ValidationException("locale", "Select a language");
		}
		User user = userRepository
			.findById(Objects.requireNonNull(userId))
			.orElseThrow(() -> new NotFoundException("User not found"));
		user.setLocale(dto.locale());
		userRepository.save(user);
		log.info(
			"UserService#updateLocale - Locale updated: userId={}, locale={}",
			userId,
			dto.locale()
		);
	}

	@Transactional
	public void updatePicture(UUID userId, MultipartFile file) {
		log.debug(
			"UserService#updatePicture - Processing picture upload for userId={}, fileName={}",
			userId,
			LogSafe.text(file.getOriginalFilename())
		);
		String encoded = encodePicture(userId, file);
		User user = userRepository
			.findById(Objects.requireNonNull(userId))
			.orElseThrow(() -> new NotFoundException("User not found"));
		user.setPicture(encoded);
		userRepository.save(user);
		log.info("UserService#updatePicture - Profile picture updated for userId={}", userId);
	}

	/**
	 * Any decoding, orientation or encoding failure is the caller's input being unprocessable, never
	 * a server fault: the pipeline answers 400 with a sentence instead of a 500 and a stack trace.
	 */
	private String encodePicture(UUID userId, MultipartFile file) {
		try {
			return processImage(file);
		} catch (ValidationException e) {
			throw e;
		} catch (RuntimeException | IOException e) {
			log.warn(
				"UserService#encodePicture - Picture could not be processed for userId={}",
				userId,
				e
			);
			throw new ValidationException("file", "The picture could not be processed");
		}
	}

	@Transactional
	public void deletePicture(UUID userId) {
		log.debug("UserService#deletePicture - Removing profile picture for userId={}", userId);
		User user = userRepository
			.findById(Objects.requireNonNull(userId))
			.orElseThrow(() -> new NotFoundException("User not found"));
		user.setPicture(null);
		userRepository.save(user);
		log.info("UserService#deletePicture - Profile picture removed for userId={}", userId);
	}

	private static final byte[] MAGIC_JPEG = { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF };
	private static final byte[] MAGIC_PNG = { (byte) 0x89, 0x50, 0x4E, 0x47 };
	private static final byte[] MAGIC_GIF = { 0x47, 0x49, 0x46, 0x38 };
	private static final byte[] MAGIC_BMP = { 0x42, 0x4D };
	private static final byte[] MAGIC_WEBP_RIFF = { 0x52, 0x49, 0x46, 0x46 };
	private static final byte[] MAGIC_WEBP_TYPE = { 0x57, 0x45, 0x42, 0x50 };

	// A 5 MB compressed image can decode to hundreds of megapixels and exhaust the
	// heap before the crop to 200x200 ever happens.
	private static final long MAX_IMAGE_PIXELS = 40_000_000L;

	private boolean isValidImageBytes(byte[] bytes) {
		if (bytes.length < 12) return false;
		if (startsWith(bytes, MAGIC_JPEG)) return true;
		if (startsWith(bytes, MAGIC_PNG)) return true;
		if (startsWith(bytes, MAGIC_GIF)) return true;
		if (startsWith(bytes, MAGIC_BMP)) return true;
		if (startsWith(bytes, MAGIC_WEBP_RIFF) && matchesAt(bytes, 8, MAGIC_WEBP_TYPE)) return true;
		return false;
	}

	private boolean startsWith(byte[] data, byte[] prefix) {
		if (data.length < prefix.length) return false;
		for (int i = 0; i < prefix.length; i++) {
			if (data[i] != prefix[i]) return false;
		}
		return true;
	}

	private boolean matchesAt(byte[] data, int offset, byte[] pattern) {
		if (data.length < offset + pattern.length) return false;
		for (int i = 0; i < pattern.length; i++) {
			if (data[offset + i] != pattern[i]) return false;
		}
		return true;
	}

	static boolean exceedsPixelLimit(long width, long height) {
		return width <= 0 || height <= 0 || width * height > MAX_IMAGE_PIXELS;
	}

	private void assertImageDimensionsWithinLimit(byte[] bytes) throws IOException {
		try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
			if (input == null) {
				throw new ValidationException(
					"file",
					"The picture must be a JPEG, PNG, GIF, BMP, or WebP image"
				);
			}

			Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
			if (!readers.hasNext()) {
				throw new ValidationException(
					"file",
					"The picture must be a JPEG, PNG, GIF, BMP, or WebP image"
				);
			}

			ImageReader reader = readers.next();
			try {
				reader.setInput(input, true, true);
				int width = reader.getWidth(0);
				int height = reader.getHeight(0);
				if (exceedsPixelLimit(width, height)) {
					log.warn(
						"UserService#processImage - Rejected oversized image: {}x{} pixels",
						width,
						height
					);
					throw new ValidationException("file", "The picture is too large, choose a smaller one");
				}
			} finally {
				reader.dispose();
			}
		}
	}

	private String processImage(MultipartFile file) throws IOException {
		log.debug(
			"UserService#processImage - Reading image bytes, originalName={}",
			LogSafe.text(file.getOriginalFilename())
		);
		byte[] bytes = file.getBytes();

		if (!isValidImageBytes(bytes)) {
			log.warn("UserService#processImage - Rejected file with invalid magic bytes");
			throw new ValidationException(
				"file",
				"The picture must be a JPEG, PNG, GIF, BMP, or WebP image"
			);
		}

		assertImageDimensionsWithinLimit(bytes);

		// Read EXIF orientation before decoding the image
		int orientation = 1;
		try {
			Metadata metadata = ImageMetadataReader.readMetadata(new ByteArrayInputStream(bytes));
			ExifIFD0Directory exif = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
			if (exif != null && exif.containsTag(ExifIFD0Directory.TAG_ORIENTATION)) {
				orientation = exif.getInt(ExifIFD0Directory.TAG_ORIENTATION);
			}
		} catch (ImageProcessingException | com.drew.metadata.MetadataException ignored) {
			// no EXIF data — keep default orientation 1
		}

		BufferedImage original = ImageIO.read(new ByteArrayInputStream(bytes));
		if (original == null) {
			log.warn("UserService#processImage - Could not decode image, unsupported format");
			throw new ValidationException(
				"file",
				"The picture must be a JPEG, PNG, GIF, BMP, or WebP image"
			);
		}

		log.debug(
			"UserService#processImage - Image decoded: {}x{}, EXIF orientation={}",
			original.getWidth(),
			original.getHeight(),
			orientation
		);

		// Apply EXIF orientation correction
		BufferedImage oriented = applyOrientation(original, orientation);

		// Center-crop to square
		int size = Math.min(oriented.getWidth(), oriented.getHeight());
		int x = (oriented.getWidth() - size) / 2;
		int y = (oriented.getHeight() - size) / 2;
		BufferedImage cropped = oriented.getSubimage(x, y, size, size);

		// Resize to 200x200
		BufferedImage resized = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = resized.createGraphics();
		g.setRenderingHint(
			RenderingHints.KEY_INTERPOLATION,
			RenderingHints.VALUE_INTERPOLATION_BICUBIC
		);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g.drawImage(cropped, 0, 0, 200, 200, null);
		g.dispose();

		// Encode as JPEG at 85% quality
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		Iterator<ImageWriter> imageWriters = ImageIO.getImageWritersByFormatName("jpeg");
		if (!imageWriters.hasNext()) {
			throw new RuntimeException("No JPEG image writer available in this JVM");
		}
		ImageWriter writer = imageWriters.next();
		ImageWriteParam param = writer.getDefaultWriteParam();
		param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
		param.setCompressionQuality(0.85f);
		try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
			writer.setOutput(ios);
			writer.write(null, new IIOImage(resized, null, null), param);
		} finally {
			writer.dispose();
		}

		return Base64.getEncoder().encodeToString(baos.toByteArray());
	}

	/**
	 * Maps the EXIF orientation onto the transform that places the source rectangle in the destination
	 * one. Drawing through Graphics2D accepts every type ImageIO decodes; an AffineTransformOp only
	 * filters a subset, and a JPEG or PNG source (TYPE_3BYTE_BGR) made it throw for 2..8.
	 */
	private BufferedImage applyOrientation(BufferedImage image, int orientation) {
		if (orientation < 2 || orientation > 8) return image;

		int w = image.getWidth();
		int h = image.getHeight();
		boolean swapDimensions = orientation >= 5;
		BufferedImage result = new BufferedImage(
			swapDimensions ? h : w,
			swapDimensions ? w : h,
			BufferedImage.TYPE_INT_RGB
		);
		Graphics2D g = result.createGraphics();
		g.setRenderingHint(
			RenderingHints.KEY_INTERPOLATION,
			RenderingHints.VALUE_INTERPOLATION_BILINEAR
		);
		g.transform(orientationTransform(orientation, w, h));
		g.drawImage(image, 0, 0, w, h, null);
		g.dispose();
		return result;
	}

	private AffineTransform orientationTransform(int orientation, int w, int h) {
		return switch (orientation) {
			// mirror horizontally
			case 2 -> new AffineTransform(-1, 0, 0, 1, w, 0);
			// rotate 180
			case 3 -> new AffineTransform(-1, 0, 0, -1, w, h);
			// mirror vertically
			case 4 -> new AffineTransform(1, 0, 0, -1, 0, h);
			// transpose: the pixel at (x, y) lands on (y, x)
			case 5 -> new AffineTransform(0, 1, 1, 0, 0, 0);
			// rotate 90 clockwise
			case 6 -> new AffineTransform(0, 1, -1, 0, h, 0);
			// transverse: the pixel at (x, y) lands on (h - y, w - x)
			case 7 -> new AffineTransform(0, -1, -1, 0, h, w);
			// rotate 90 counter-clockwise
			case 8 -> new AffineTransform(0, -1, 1, 0, 0, w);
			default -> new AffineTransform();
		};
	}

	@Transactional(readOnly = true)
	public UserExportDTO exportUserData(UUID userId, String email) {
		log.info("UserService#exportUserData - Exporting data for userId={}", userId);
		User user = userRepository
			.findById(Objects.requireNonNull(userId))
			.orElseThrow(() -> new NotFoundException("User not found"));

		var profile = new UserExportDTO.ProfileExport(
			user.getId(),
			email,
			user.getCurrency().name(),
			user.getLocale().name()
		);

		var categories = categoryRepository
			.findByUserId(userId)
			.stream()
			.map(c -> new UserExportDTO.CategoryExport(c.getId(), c.getName(), c.getType().name()))
			.toList();

		var assets = assetRepository
			.findByUserIdOrderByCreatedAtDesc(userId)
			.stream()
			.map(a -> {
				var snapshots = a
					.getSnapshots()
					.stream()
					.map(s ->
						new UserExportDTO.SnapshotExport(
							s.getId(),
							s.getReferenceDate(),
							s.getAmountOriginalCurrency(),
							s.getExchangeRateToBase(),
							s.getSource().name()
						)
					)
					.toList();
				return new UserExportDTO.AssetExport(
					a.getId(),
					a.getName(),
					a.getCategory() != null ? a.getCategory().getName() : null,
					a.getOriginalCurrency().name(),
					a.getIsFavorite(),
					a.getLiabilityType() != null ? a.getLiabilityType().name() : null,
					a.getPaymentFrequency() != null ? a.getPaymentFrequency().name() : null,
					a.getPaymentAmount(),
					a.getCreatedAt(),
					snapshots
				);
			})
			.toList();

		var targets = targetRepository
			.findByUserId(userId)
			.stream()
			.map(t ->
				new UserExportDTO.TargetExport(
					t.getId(),
					t.getName(),
					t.getTargetAmount(),
					t.getCurrentAmount(),
					t.getType().name(),
					t.getDeadline(),
					t.getIsCompleted()
				)
			)
			.toList();

		var cashFlows = cashFlowRepository
			.findByUserIdOrderByYearDescMonthAsc(userId)
			.stream()
			.map(cf ->
				new UserExportDTO.CashFlowExport(
					cf.getId(),
					cf.getYear(),
					cf.getMonth(),
					cf.getEarned(),
					cf.getSpent()
				)
			)
			.toList();

		return new UserExportDTO(profile, categories, assets, targets, cashFlows);
	}

	public boolean isGoogleUser(UserPrincipal principal) {
		return principal.user().getJwtId().startsWith("google-oauth2|");
	}
}
