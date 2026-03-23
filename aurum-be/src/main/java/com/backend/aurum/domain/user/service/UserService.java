package com.backend.aurum.domain.user.service;

import com.auth0.client.mgmt.ManagementAPI;
import com.auth0.exception.Auth0Exception;
import com.backend.aurum.domain.analytics.repository.TargetRepository;
import com.backend.aurum.domain.asset.repository.AssetCategoryRepository;
import com.backend.aurum.domain.asset.repository.AssetRepository;
import com.backend.aurum.domain.auth.service.Auth0ManagementService;
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
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

	public User findOrCreate(String jwtId) {
		log.debug("UserService#findOrCreate - Looking up user by jwtId={}", jwtId);
		return userRepository
			.findByJwtId(jwtId)
			.orElseGet(() -> {
				log.info(
					"UserService#findOrCreate - No existing user found, creating new user for jwtId={}",
					jwtId
				);
				try {
					User newUser = new User();
					newUser.setJwtId(jwtId);
					newUser.setCurrency(Currency.EUR);
					newUser.setLocale(Locale.EN_US);
					User created = userRepository.saveAndFlush(newUser);
					log.info("UserService#findOrCreate - New user created: userId={}", created.getId());
					return created;
				} catch (Exception e) {
					log.warn(
						"UserService#findOrCreate - Concurrent insert detected for jwtId={}, retrying lookup",
						jwtId
					);
					return userRepository
						.findByJwtId(jwtId)
						.orElseThrow(() -> new RuntimeException("Failed to find or create user", e));
				}
			});
	}

	@Transactional
	public User getUserById(UUID userId) {
		log.debug("UserService#getUserById - Fetching user: userId={}", userId);
		return userRepository.findById(Objects.requireNonNull(userId)).orElseThrow();
	}

	@Transactional
	public void deleteUser(UUID userId, String jwtId) {
		log.info("UserService#deleteUser - Deleting user: userId={}, jwtId={}", userId, jwtId);
		userRepository.deleteById(Objects.requireNonNull(userId));
		log.debug(
			"UserService#deleteUser - Local user record deleted, proceeding to delete Auth0 user"
		);

		try {
			ManagementAPI managementApi = auth0ManagementService.getManagementAPI();
			managementApi.users().delete(jwtId).execute();
			log.info("UserService#deleteUser - Auth0 user deleted successfully: jwtId={}", jwtId);
		} catch (Auth0Exception e) {
			log.error("UserService#deleteUser - Failed to delete Auth0 user: jwtId={}", jwtId, e);
			throw new RuntimeException("Error during Auth0 user deletion", e);
		}
	}

	@Transactional
	public void updateAuth0Name(String jwtId, UpdateNameDTO dto) {
		log.debug("UserService#updateAuth0Name - Updating name for jwtId={}", jwtId);
		try {
			ManagementAPI mgmt = auth0ManagementService.getManagementAPI();
			com.auth0.json.mgmt.users.User auth0User = new com.auth0.json.mgmt.users.User();
			auth0User.setName(dto.name());
			mgmt.users().update(jwtId, auth0User).execute();
			log.info("UserService#updateAuth0Name - Name updated in Auth0 for jwtId={}", jwtId);
		} catch (Auth0Exception e) {
			log.error("UserService#updateAuth0Name - Failed to update name in Auth0: jwtId={}", jwtId, e);
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
		User user = userRepository.findById(Objects.requireNonNull(userId)).orElseThrow();
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
		User user = userRepository.findById(Objects.requireNonNull(userId)).orElseThrow();
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
			file.getOriginalFilename()
		);
		try {
			String encoded = processImage(file);
			User user = userRepository.findById(Objects.requireNonNull(userId)).orElseThrow();
			user.setPicture(encoded);
			userRepository.save(user);
			log.info("UserService#updatePicture - Profile picture updated for userId={}", userId);
		} catch (IOException e) {
			log.error("UserService#updatePicture - Failed to process image for userId={}", userId, e);
			throw new RuntimeException("Failed to process image", e);
		}
	}

	@Transactional
	public void deletePicture(UUID userId) {
		log.debug("UserService#deletePicture - Removing profile picture for userId={}", userId);
		User user = userRepository.findById(Objects.requireNonNull(userId)).orElseThrow();
		user.setPicture(null);
		userRepository.save(user);
		log.info("UserService#deletePicture - Profile picture removed for userId={}", userId);
	}

	private String processImage(MultipartFile file) throws IOException {
		log.debug(
			"UserService#processImage - Reading image bytes, originalName={}",
			file.getOriginalFilename()
		);
		byte[] bytes = file.getBytes();

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
			throw new IllegalArgumentException("Invalid or unsupported image format");
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
		ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
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

	private BufferedImage applyOrientation(BufferedImage image, int orientation) {
		if (orientation == 1) return image;

		int w = image.getWidth();
		int h = image.getHeight();
		boolean swapDimensions = orientation >= 5;
		int outW = swapDimensions ? h : w;
		int outH = swapDimensions ? w : h;

		AffineTransform t = new AffineTransform();
		switch (orientation) {
			case 2 -> {
				t.scale(-1, 1);
				t.translate(-w, 0);
			}
			case 3 -> {
				t.translate(w, h);
				t.rotate(Math.PI);
			}
			case 4 -> {
				t.scale(1, -1);
				t.translate(0, -h);
			}
			case 5 -> {
				t.rotate(-Math.PI / 2);
				t.scale(-1, 1);
			}
			case 6 -> {
				t.translate(h, 0);
				t.rotate(Math.PI / 2);
			}
			case 7 -> {
				t.scale(-1, 1);
				t.translate(-h, 0);
				t.translate(0, w);
				t.rotate((3 * Math.PI) / 2);
			}
			case 8 -> {
				t.translate(0, w);
				t.rotate(-Math.PI / 2);
			}
			default -> {
				return image;
			}
		}

		BufferedImage result = new BufferedImage(outW, outH, BufferedImage.TYPE_INT_RGB);
		new AffineTransformOp(t, AffineTransformOp.TYPE_BICUBIC).filter(image, result);
		return result;
	}

	@Transactional(readOnly = true)
	public UserExportDTO exportUserData(UUID userId, String email) {
		log.info("UserService#exportUserData - Exporting data for userId={}", userId);
		User user = userRepository.findById(Objects.requireNonNull(userId)).orElseThrow();

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
							s.getExchangeRateToBase()
						)
					)
					.toList();
				return new UserExportDTO.AssetExport(
					a.getId(),
					a.getName(),
					a.getCategory() != null ? a.getCategory().getName() : null,
					a.getOriginalCurrency().name(),
					a.getIsActive(),
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

		return new UserExportDTO(profile, categories, assets, targets);
	}

	public boolean isGoogleUser(UserPrincipal principal) {
		return principal.user().getJwtId().startsWith("google-oauth2|");
	}
}
