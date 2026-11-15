package com.backend.aurum.domain.user.service;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.zip.CRC32;
import javax.imageio.ImageIO;

/**
 * Builds the picture bytes the upload pipeline is asserted on. A square canvas split into four
 * differently coloured quadrants makes a wrong EXIF orientation visible as a colour in the wrong
 * corner, and every byte is produced at runtime: a committed binary fixture would hide which
 * orientation it actually carries.
 */
final class ProfilePictureFixture {

	static final int SOURCE_SIZE = 400;

	private static final int SOURCE_HALF = SOURCE_SIZE / 2;

	private static final int QUADRANTS = 4;

	private static final byte[] TIFF_LITTLE_ENDIAN_HEADER = { 'I', 'I', 42, 0, 8, 0, 0, 0 };

	private static final int ORIENTATION_TAG = 0x0112;

	private static final int TIFF_TYPE_SHORT = 3;

	private static final int ONE_ENTRY = 1;

	private static final int ONE_VALUE = 1;

	private static final int NO_VALUE_PADDING = 0;

	private static final int NO_NEXT_IFD = 0;

	private static final byte[] EXIF_APP1_MARKER = { (byte) 0xFF, (byte) 0xE1 };

	private static final byte[] EXIF_SIGNATURE = { 'E', 'x', 'i', 'f', 0, 0 };

	private static final byte[] EXIF_CHUNK_TYPE = { 'e', 'X', 'I', 'f' };

	private static final int JPEG_SEGMENT_LENGTH_BYTES = 2;

	private static final int JPEG_SEGMENT_OFFSET = 2;

	private static final int PNG_IHDR_END_OFFSET = 33;

	private static final int START_OF_SCAN_MARKER = 0xFFDA;

	private ProfilePictureFixture() {}

	enum Format {
		JPEG("jpeg") {
			@Override
			byte[] withExif(byte[] image, byte[] tiff) {
				return insert(image, JPEG_SEGMENT_OFFSET, app1Segment(tiff));
			}
		},
		PNG("png") {
			@Override
			byte[] withExif(byte[] image, byte[] tiff) {
				return insert(image, PNG_IHDR_END_OFFSET, exifChunk(tiff));
			}
		},
		BMP("bmp") {
			@Override
			byte[] withExif(byte[] image, byte[] tiff) {
				return image;
			}

			@Override
			boolean carriesExif() {
				return false;
			}
		};

		private final String imageIoName;

		Format(String imageIoName) {
			this.imageIoName = imageIoName;
		}

		String imageIoName() {
			return imageIoName;
		}

		boolean carriesExif() {
			return true;
		}

		abstract byte[] withExif(byte[] image, byte[] tiff);
	}

	enum Corner {
		TOP_LEFT(false, false),
		TOP_RIGHT(true, false),
		BOTTOM_LEFT(false, true),
		BOTTOM_RIGHT(true, true);

		private final boolean right;
		private final boolean bottom;

		Corner(boolean right, boolean bottom) {
			this.right = right;
			this.bottom = bottom;
		}

		boolean right() {
			return right;
		}

		boolean bottom() {
			return bottom;
		}
	}

	enum Quadrant {
		RED(255, 0, 0),
		GREEN(0, 255, 0),
		BLUE(0, 0, 255),
		WHITE(255, 255, 255);

		private final Color color;

		Quadrant(int red, int green, int blue) {
			this.color = new Color(red, green, blue);
		}

		Color color() {
			return color;
		}

		static Quadrant nearest(Color sample) {
			return Arrays.stream(values())
				.min(Comparator.comparingInt(quadrant -> quadrant.squaredDistance(sample)))
				.orElseThrow();
		}

		private int squaredDistance(Color sample) {
			int redDelta = sample.getRed() - color.getRed();
			int greenDelta = sample.getGreen() - color.getGreen();
			int blueDelta = sample.getBlue() - color.getBlue();
			return redDelta * redDelta + greenDelta * greenDelta + blueDelta * blueDelta;
		}
	}

	record QuadrantLayout(
		Quadrant topLeft,
		Quadrant topRight,
		Quadrant bottomLeft,
		Quadrant bottomRight
	) {
		static final QuadrantLayout SOURCE = new QuadrantLayout(
			Quadrant.RED,
			Quadrant.GREEN,
			Quadrant.BLUE,
			Quadrant.WHITE
		);

		Quadrant at(Corner corner) {
			return switch (corner) {
				case TOP_LEFT -> topLeft;
				case TOP_RIGHT -> topRight;
				case BOTTOM_LEFT -> bottomLeft;
				case BOTTOM_RIGHT -> bottomRight;
			};
		}
	}

	/**
	 * A source picture stamped with the given EXIF orientation. Formats without an EXIF channel (BMP)
	 * return the plain file: the orientation never reaches the pipeline through them.
	 */
	static byte[] picture(Format format, int exifOrientation) throws IOException {
		byte[] plain = encode(sourceImage(), format.imageIoName());
		if (!format.carriesExif()) {
			return plain;
		}
		return format.withExif(plain, orientationTiff(exifOrientation));
	}

	/**
	 * A JPEG whose headers are intact and whose scan data is gone: the reader knows the picture is
	 * 400x400 and then fails, which is the corrupt upload the pipeline must answer as a 400.
	 */
	static byte[] jpegWithoutScanData() throws IOException {
		byte[] jpeg = encode(sourceImage(), Format.JPEG.imageIoName());
		return Arrays.copyOf(jpeg, indexOfStartOfScan(jpeg));
	}

	/** The quadrant that landed in the given corner of a stored picture, read at its centre. */
	static Quadrant quadrantOf(BufferedImage picture, Corner corner) {
		int x = corner.right() ? (3 * picture.getWidth()) / QUADRANTS : picture.getWidth() / QUADRANTS;
		int y = corner.bottom()
			? (3 * picture.getHeight()) / QUADRANTS
			: picture.getHeight() / QUADRANTS;
		return Quadrant.nearest(new Color(picture.getRGB(x, y)));
	}

	private static BufferedImage sourceImage() {
		BufferedImage image = new BufferedImage(SOURCE_SIZE, SOURCE_SIZE, BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics = image.createGraphics();
		for (Corner corner : Corner.values()) {
			graphics.setColor(QuadrantLayout.SOURCE.at(corner).color());
			int x = corner.right() ? SOURCE_HALF : 0;
			int y = corner.bottom() ? SOURCE_HALF : 0;
			graphics.fillRect(x, y, SOURCE_HALF, SOURCE_HALF);
		}
		graphics.dispose();
		return image;
	}

	private static byte[] encode(BufferedImage image, String formatName) throws IOException {
		ByteArrayOutputStream encoded = new ByteArrayOutputStream();
		ImageIO.write(image, formatName, encoded);
		return encoded.toByteArray();
	}

	private static byte[] orientationTiff(int orientation) {
		ByteArrayOutputStream tiff = new ByteArrayOutputStream();
		tiff.writeBytes(TIFF_LITTLE_ENDIAN_HEADER);
		writeLittleEndianShort(tiff, ONE_ENTRY);
		writeLittleEndianShort(tiff, ORIENTATION_TAG);
		writeLittleEndianShort(tiff, TIFF_TYPE_SHORT);
		writeLittleEndianInt(tiff, ONE_VALUE);
		writeLittleEndianShort(tiff, orientation);
		writeLittleEndianShort(tiff, NO_VALUE_PADDING);
		writeLittleEndianInt(tiff, NO_NEXT_IFD);
		return tiff.toByteArray();
	}

	private static byte[] app1Segment(byte[] tiff) {
		ByteArrayOutputStream body = new ByteArrayOutputStream();
		body.writeBytes(EXIF_SIGNATURE);
		body.writeBytes(tiff);
		byte[] payload = body.toByteArray();
		int length = payload.length + JPEG_SEGMENT_LENGTH_BYTES;
		ByteArrayOutputStream segment = new ByteArrayOutputStream();
		segment.writeBytes(EXIF_APP1_MARKER);
		segment.write((length >> 8) & 0xFF);
		segment.write(length & 0xFF);
		segment.writeBytes(payload);
		return segment.toByteArray();
	}

	private static byte[] exifChunk(byte[] tiff) {
		ByteArrayOutputStream chunk = new ByteArrayOutputStream();
		writeBigEndianInt(chunk, tiff.length);
		chunk.writeBytes(EXIF_CHUNK_TYPE);
		chunk.writeBytes(tiff);
		CRC32 crc = new CRC32();
		crc.update(EXIF_CHUNK_TYPE);
		crc.update(tiff);
		writeBigEndianInt(chunk, (int) crc.getValue());
		return chunk.toByteArray();
	}

	private static byte[] insert(byte[] image, int offset, byte[] inserted) {
		ByteArrayOutputStream framed = new ByteArrayOutputStream();
		framed.write(image, 0, offset);
		framed.writeBytes(inserted);
		framed.write(image, offset, image.length - offset);
		return framed.toByteArray();
	}

	private static int indexOfStartOfScan(byte[] jpeg) {
		for (int index = JPEG_SEGMENT_OFFSET; index < jpeg.length - 1; index++) {
			int marker = ((jpeg[index] & 0xFF) << 8) | (jpeg[index + 1] & 0xFF);
			if (marker == START_OF_SCAN_MARKER) {
				return index;
			}
		}
		throw new IllegalStateException("the encoded JPEG carries no start-of-scan marker");
	}

	private static void writeLittleEndianShort(ByteArrayOutputStream out, int value) {
		out.write(value & 0xFF);
		out.write((value >> 8) & 0xFF);
	}

	private static void writeLittleEndianInt(ByteArrayOutputStream out, int value) {
		out.write(value & 0xFF);
		out.write((value >> 8) & 0xFF);
		out.write((value >> 16) & 0xFF);
		out.write((value >> 24) & 0xFF);
	}

	private static void writeBigEndianInt(ByteArrayOutputStream out, int value) {
		out.write((value >> 24) & 0xFF);
		out.write((value >> 16) & 0xFF);
		out.write((value >> 8) & 0xFF);
		out.write(value & 0xFF);
	}
}
