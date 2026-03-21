package com.backend.aurum.domain.user.service;

import com.auth0.client.mgmt.ManagementAPI;
import com.auth0.exception.Auth0Exception;
import com.backend.aurum.domain.auth.service.Auth0ManagementService;
import com.backend.aurum.domain.user.dto.UpdateCurrencyDTO;
import com.backend.aurum.domain.user.dto.UpdateLocaleDTO;
import com.backend.aurum.domain.user.dto.UpdateNameDTO;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class UserService {

	private final UserRepository userRepository;
	private final Auth0ManagementService auth0ManagementService;

	public User findOrCreate(String jwtId, String email) {
		return userRepository
			.findByJwtId(jwtId)
			.orElseGet(() -> {
				try {
					User newUser = new User();
					newUser.setJwtId(jwtId);
					newUser.setEmail(email);
					newUser.setCurrency(Currency.EUR);
					newUser.setLocale(Locale.EN_US);
					return userRepository.saveAndFlush(newUser);
				} catch (Exception e) {
					return userRepository
						.findByJwtId(jwtId)
						.orElseThrow(() -> new RuntimeException("Failed to find or create user", e));
				}
			});
	}

	@Transactional
	public User getUserById(UUID userId) {
		return userRepository.findById(Objects.requireNonNull(userId)).orElseThrow();
	}

	@Transactional
	public void deleteUser(UUID userId, String jwtId) {
		userRepository.deleteById(Objects.requireNonNull(userId));

		try {
			ManagementAPI managementApi = auth0ManagementService.getManagementAPI();
			managementApi.users().delete(jwtId).execute();
		} catch (Auth0Exception e) {
			throw new RuntimeException("Error during Auth0 user deletion", e);
		}
	}

	@Transactional
	public void updateAuth0Name(String jwtId, UpdateNameDTO dto) {
		try {
			ManagementAPI mgmt = auth0ManagementService.getManagementAPI();
			com.auth0.json.mgmt.users.User auth0User = new com.auth0.json.mgmt.users.User();
			auth0User.setName(dto.name());
			mgmt.users().update(jwtId, auth0User).execute();
		} catch (Auth0Exception e) {
			throw new RuntimeException("Error during Auth0 name update: " + e.getMessage());
		}
	}

	@Transactional
	public void updateCurrency(UUID userId, UpdateCurrencyDTO dto) {
		User user = userRepository.findById(Objects.requireNonNull(userId)).orElseThrow();
		user.setCurrency(dto.currency());
		userRepository.save(user);
	}

	@Transactional
	public void updateLocale(UUID userId, UpdateLocaleDTO dto) {
		User user = userRepository.findById(Objects.requireNonNull(userId)).orElseThrow();
		user.setLocale(dto.locale());
		userRepository.save(user);
	}

	@Transactional
	public void updatePicture(UUID userId, MultipartFile file) {
		try {
			String encoded = processImage(file);
			User user = userRepository.findById(Objects.requireNonNull(userId)).orElseThrow();
			user.setPicture(encoded);
			userRepository.save(user);
		} catch (IOException e) {
			throw new RuntimeException("Failed to process image", e);
		}
	}

	@Transactional
	public void deletePicture(UUID userId) {
		User user = userRepository.findById(Objects.requireNonNull(userId)).orElseThrow();
		user.setPicture(null);
		userRepository.save(user);
	}

	private String processImage(MultipartFile file) throws IOException {
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
			throw new IllegalArgumentException("Invalid or unsupported image format");
		}

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

	public boolean isGoogleUser(UserPrincipal principal) {
		return principal.user().getJwtId().startsWith("google-oauth2|");
	}
}
