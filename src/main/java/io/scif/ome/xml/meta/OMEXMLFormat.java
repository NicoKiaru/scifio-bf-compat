/*
 * #%L
 * SCIFIO Bio-Formats compatibility format.
 * %%
 * Copyright (C) 2013 Open Microscopy Environment:
 *   - Board of Regents of the University of Wisconsin-Madison
 *   - Glencoe Software, Inc.
 *   - University of Dundee
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */

package io.scif.ome.xml.meta;

import io.scif.AbstractChecker;
import io.scif.AbstractFormat;
import io.scif.AbstractMetadata;
import io.scif.AbstractParser;
import io.scif.AbstractTranslator;
import io.scif.AbstractWriter;
import io.scif.ByteArrayPlane;
import io.scif.ByteArrayReader;
import io.scif.FormatException;
import io.scif.MissingLibraryException;
import io.scif.Plane;
import io.scif.Translator;
import io.scif.codec.Base64Codec;
import io.scif.codec.CodecOptions;
import io.scif.codec.CompressionType;
import io.scif.codec.JPEG2000Codec;
import io.scif.codec.JPEGCodec;
import io.scif.codec.ZlibCodec;
import io.scif.common.Constants;
import io.scif.io.CBZip2InputStream;
import io.scif.io.RandomAccessInputStream;
import io.scif.ome.xml.services.OMEXMLMetadataService;
import io.scif.ome.xml.services.OMEXMLService;
import io.scif.ome.xml.services.OMEXMLServiceImpl;
import io.scif.ome.xml.translation.FromOMETranslator;
import io.scif.services.ServiceException;
import io.scif.util.FormatTools;
import io.scif.util.ImageTools;
import io.scif.xml.BaseHandler;
import io.scif.xml.XMLTools;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Vector;

import loci.formats.meta.MetadataRetrieve;
import net.imglib2.meta.Axes;

import org.scijava.Priority;
import org.scijava.plugin.Attr;
import org.scijava.plugin.Plugin;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Format for OME-XML files.
 * 
 * @author Melissa Linkert melissa at glencoesoftware.com
 * @author Mark Hiner hinerm at gmail.com
 */
@Plugin(type = OMEXMLFormat.class)
public class OMEXMLFormat extends AbstractFormat {

	// -- Static fields --

	private static boolean noOME = false;

	static {
		try {
			Class.forName("ome.xml.OMEXMLNode");
		}
		catch (final Throwable t) {
			noOME = true;
			LOGGER.debug(OMEXMLServiceImpl.NO_OME_XML_MSG, t);
		}
	}

	// -- Format API Methods --

	/*
	 * @see io.scif.Format#getFormatName()
	 */
	public String getFormatName() {
		return "OME-XML";
	}

	/*
	 * @see io.scif.Format#getSuffixes()
	 */
	public String[] getSuffixes() {
		return new String[] { "ome" };
	}

	// -- Nested Classes --

	/**
	 * io.scif.Metadata class wrapping an OME-XML root.
	 * 
	 * @see ome.xml.meta.OMEXMLMetadata
	 * @see io.scif.Metadata
	 * @author Mark Hiner
	 */
	public static class Metadata extends AbstractMetadata {

		// -- Constants --

		public static final String FORMAT_NAME = "OME-XML";
		public static final String CNAME =
			"io.scif.ome.xml.meta.OMEXMLFormat$Metadata";

		// -- Fields --

		/** OME core */
		protected OMEMetadata omeMeta;

		// compression value and offset for each BinData element
		private Vector<BinData> binData;
		private Vector<Long> binDataOffsets;
		private Vector<String> compression;

		private String omexml;
		private boolean hasSPW = false;

		// -- OMEXMLMetadata getters and setters --

		public void setOMEMeta(final OMEMetadata ome) {
			omeMeta = ome;
		}

		public OMEMetadata getOMEMeta() {
			return omeMeta;
		}

		public Vector<BinData> getBinData() {
			return binData;
		}

		public void setBinData(final Vector<BinData> binData) {
			this.binData = binData;
		}

		public Vector<Long> getBinDataOffsets() {
			return binDataOffsets;
		}

		public void setBinDataOffsets(final Vector<Long> binDataOffsets) {
			this.binDataOffsets = binDataOffsets;
		}

		public Vector<String> getCompression() {
			return compression;
		}

		public void setCompression(final Vector<String> compression) {
			this.compression = compression;
		}

		public String getOmexml() {
			return omexml;
		}

		public void setOmexml(final String omexml) {
			this.omexml = omexml;
		}

		public boolean isSPW() {
			return hasSPW;
		}

		public void setSPW(final boolean hasSPW) {
			this.hasSPW = hasSPW;
		}

		// -- Metadata API Methods --

		/*
		 * @see io.scif.AbstractMetadata#getFormatName()
		 */
		public String getFormatName() {
			return FORMAT_NAME;
		}

		/*
		 * @see io.scif.AbstractMetadata#populateImageMetadata()
		 */
		public void populateImageMetadata() {
			getContext().getService(OMEXMLMetadataService.class).populateMetadata(
				getOMEMeta().getRoot(), this);

			for (int i = 0; i < getImageCount(); i++) {
				setRGB(i, false);
				setInterleaved(i, false);
				setIndexed(i, false);
				setFalseColor(i, true);
				get(i).setPlaneCount(
					getAxisLength(i, Axes.CHANNEL) * getAxisLength(i, Axes.Z) *
						getAxisLength(i, Axes.TIME));
			}
		}

		@Override
		public void close(final boolean fileOnly) throws IOException {
			super.close(fileOnly);
			if (!fileOnly) {
				compression = null;
				binDataOffsets = null;
				binData = null;
				omexml = null;
				hasSPW = false;
			}
		}
	}

	/**
	 * @author Mark Hiner hinerm at gmail.com
	 */
	public static class Checker extends AbstractChecker {

		// -- Constructor --

		public Checker() {
			suffixNecessary = false;
		}

		@Override
		public boolean isFormat(final RandomAccessInputStream stream)
			throws IOException
		{
			final int blockLen = 64;
			final String xml = stream.readString(blockLen);
			return xml.startsWith("<?xml") && xml.indexOf("<OME") >= 0;
		}
	}

	/**
	 * @author Mark Hiner hinerm at gmail.com
	 */
	public static class Parser extends AbstractParser<Metadata> {

		// -- Parser API Methods --

		@Override
		protected void typedParse(final RandomAccessInputStream stream,
			final Metadata meta) throws IOException, FormatException
		{
			if (noOME) {
				throw new MissingLibraryException(OMEXMLServiceImpl.NO_OME_XML_MSG);
			}

			final Vector<BinData> binData = new Vector<BinData>();
			final Vector<Long> binDataOffsets = new Vector<Long>();
			final Vector<String> compression = new Vector<String>();
			meta.setBinData(binData);
			meta.setBinDataOffsets(binDataOffsets);
			meta.setCompression(compression);

			final DefaultHandler handler = new OMEXMLHandler(meta);
			try {
				final RandomAccessInputStream s =
					new RandomAccessInputStream(getContext(), stream.getFileName());
				XMLTools.parseXML(s, handler);
				s.close();
			}
			catch (final IOException e) {
				throw new FormatException("Malformed OME-XML", e);
			}

			int lineNumber = 1;
			for (final BinData bin : binData) {
				final int line = bin.getRow();
				final int col = bin.getColumn();

				while (lineNumber < line) {
					in.readLine();
					lineNumber++;
				}
				binDataOffsets.add(stream.getFilePointer() + col - 1);
			}

			if (binDataOffsets.size() == 0) {
				throw new FormatException("Pixel data not found");
			}

			LOGGER.info("Populating metadata");

			final OMEMetadata omeMeta = meta.getOMEMeta();
			OMEXMLMetadata omexmlMeta = null;
			if (omeMeta != null) omexmlMeta = meta.getOMEMeta().getRoot();
			final OMEXMLService service =
				scifio().format().getInstance(OMEXMLService.class);

			try {

				if (omexmlMeta == null) {
					omexmlMeta = service.createOMEXMLMetadata(meta.getOmexml());
					meta.setOMEMeta(new OMEMetadata(getContext(), omexmlMeta));
				}

				service.convertMetadata(meta.getOmexml(), omexmlMeta);

			}
			catch (final ServiceException se) {
				throw new FormatException(se);
			}

			for (int i = 0; i < omexmlMeta.getImageCount(); i++)
				omexmlMeta.setImageName(stream.getFileName(), i);

			meta.setSPW(omexmlMeta.getPlateCount() > 0);
			addGlobalMeta("Is SPW file", meta.isSPW());
		}
	}

	/**
	 * @author Mark Hiner hinerm at gmail.com
	 */
	public static class Reader extends ByteArrayReader<Metadata> {

		// -- Constructor --

		public Reader() {
			domains = FormatTools.NON_GRAPHICS_DOMAINS;
		}

		// -- Reader API Methods --

		/*
		 * @see io.scif.TypedReader#openPlane(int, int, io.scif.DataPlane, int, int, int, int)
		 */
		public ByteArrayPlane openPlane(final int imageIndex, final int planeIndex,
			final ByteArrayPlane plane, final int x, final int y, final int w,
			final int h) throws FormatException, IOException
		{
			final byte[] buf = plane.getBytes();
			final Metadata meta = getMetadata();

			FormatTools.checkPlaneParameters(this, imageIndex, planeIndex,
				buf.length, x, y, w, h);

			int index = planeIndex;

			for (int i = 0; i < imageIndex; i++) {
				index += meta.getPlaneCount(i);
			}
			if (index >= meta.getBinDataOffsets().size()) {
				index = meta.getBinDataOffsets().size() - 1;
			}

			final long offset = meta.getBinDataOffsets().get(index).longValue();
			final String compress = meta.getCompression().get(index);

			getStream().seek(offset);

			final int depth =
				FormatTools.getBytesPerPixel(meta.getPixelType(imageIndex));
			final int planeSize =
				meta.getAxisLength(imageIndex, Axes.X) *
					meta.getAxisLength(imageIndex, Axes.Y) * depth;

			final CodecOptions options = new CodecOptions();
			options.width = meta.getAxisLength(imageIndex, Axes.X);
			options.height = meta.getAxisLength(imageIndex, Axes.Y);
			options.bitsPerSample = depth * 8;
			options.channels = meta.getRGBChannelCount(imageIndex);
			options.maxBytes = planeSize;
			options.littleEndian = meta.isLittleEndian(imageIndex);
			options.interleaved = meta.isInterleaved(imageIndex);

			byte[] pixels = new Base64Codec().decompress(getStream(), options);

			// return a blank plane if no pixel data was stored
			if (pixels.length == 0) {
				LOGGER.debug("No pixel data for plane #{}", planeIndex);
				return plane;
			}

			// TODO: Create a method uncompress to handle all compression methods
			if (compress.equals("bzip2")) {
				byte[] tempPixels = pixels;
				pixels = new byte[tempPixels.length - 2];
				System.arraycopy(tempPixels, 2, pixels, 0, pixels.length);

				ByteArrayInputStream bais = new ByteArrayInputStream(pixels);
				CBZip2InputStream bzip = new CBZip2InputStream(bais);
				pixels = new byte[planeSize];
				bzip.read(pixels, 0, pixels.length);
				tempPixels = null;
				bais.close();
				bais = null;
				bzip = null;
			}
			else if (compress.equals("zlib")) {
				pixels = new ZlibCodec().decompress(pixels, options);
			}
			else if (compress.equals("J2K")) {
				pixels = new JPEG2000Codec().decompress(pixels, options);
			}
			else if (compress.equals("JPEG")) {
				pixels = new JPEGCodec().decompress(pixels, options);
			}

			for (int row = 0; row < h; row++) {
				final int off =
					(row + y) * meta.getAxisLength(imageIndex, Axes.X) * depth + x *
						depth;
				System.arraycopy(pixels, off, buf, row * w * depth, w * depth);
			}

			pixels = null;

			return plane;
		}

		@Override
		public String[] getDomains() {
			FormatTools.assertId(currentId, true, 1);
			return getMetadata().isSPW() ? new String[] { FormatTools.HCS_DOMAIN }
				: FormatTools.NON_SPECIAL_DOMAINS;
		}
	}

	/**
	 * @author Mark Hiner hinerm at gmail.com
	 */
	public static class Writer extends AbstractWriter<Metadata> {

		// -- Fields --

		private Vector<String> xmlFragments;
		private String currentFragment;
		private OMEXMLService service;

		// -- Constructor --

		public Writer() {
			compressionTypes =
				new String[] { CompressionType.UNCOMPRESSED.getCompression(),
					CompressionType.ZLIB.getCompression() };
			compression = compressionTypes[0];
		}

		// -- Writer API Methods --

		/*
		 * @see io.scif.Writer#savePlane(int, int, io.scif.Plane, int, int, int, int)
		 */
		public void savePlane(final int imageIndex, final int planeIndex,
			final Plane plane, final int x, final int y, final int w, final int h)
			throws FormatException, IOException
		{
			final Metadata meta = getMetadata();
			final byte[] buf = plane.getBytes();

			checkParams(imageIndex, planeIndex, buf, x, y, w, h);
			if (!isFullPlane(imageIndex, x, y, w, h)) {
				throw new FormatException(
					"OMEXMLWriter does not yet support saving image tiles.");
			}
			final MetadataRetrieve retrieve = meta.getOMEMeta().getRoot();

			if (planeIndex == 0) {
				out.writeBytes(xmlFragments.get(imageIndex));
			}

			final String type = retrieve.getPixelsType(imageIndex).toString();
			final int pixelType = FormatTools.pixelTypeFromString(type);
			final int bytes = FormatTools.getBytesPerPixel(pixelType);
			final int nChannels = meta.getRGBChannelCount(imageIndex);
			final int sizeX =
				retrieve.getPixelsSizeX(imageIndex).getValue().intValue();
			final int sizeY =
				retrieve.getPixelsSizeY(imageIndex).getValue().intValue();
			final int planeSize = sizeX * sizeY * bytes;
			final boolean bigEndian =
				retrieve.getPixelsBinDataBigEndian(imageIndex, 0);

			final String namespace =
				"xmlns=\"http://www.openmicroscopy.org/Schemas/BinaryFile/" +
					service.getLatestVersion() + "\"";

			for (int i = 0; i < nChannels; i++) {
				final byte[] b =
					ImageTools
						.splitChannels(buf, i, nChannels, bytes, false, interleaved);
				final byte[] encodedPix = compress(b, imageIndex);

				final StringBuffer omePlane = new StringBuffer("\n<BinData ");
				omePlane.append(namespace);
				omePlane.append(" Length=\"");
				omePlane.append(planeSize);
				omePlane.append("\"");
				omePlane.append(" BigEndian=\"");
				omePlane.append(bigEndian);
				omePlane.append("\"");
				if (compression != null && !compression.equals("Uncompressed")) {
					omePlane.append(" Compression=\"");
					omePlane.append(compression);
					omePlane.append("\"");
				}
				omePlane.append(">");
				omePlane.append(new String(encodedPix, Constants.ENCODING));
				omePlane.append("</BinData>");
				out.writeBytes(omePlane.toString());
			}
		}

		@Override
		public void setMetadata(final Metadata meta) throws FormatException {
			super.setMetadata(meta);
			final MetadataRetrieve retrieve = meta.getOMEMeta().getRoot();

			String xml;
			try {
				service = scifio().format().getInstance(OMEXMLService.class);
				xml = service.getOMEXML(retrieve);
				final OMEXMLMetadata noBin = service.createOMEXMLMetadata(xml);
				service.removeBinData(noBin);
				xml = service.getOMEXML(noBin);
			}
			catch (final ServiceException se) {
				throw new FormatException(se);
			}

			final OMEHandler handler =
				new OMEHandler(new Vector<String>(),
					"<?xml version=\"1.0\" encoding=\"UTF-8\"?>");

			try {
				XMLTools.parseXML(xml, handler);
			}
			catch (final IOException e) {
				throw new FormatException(e);
			}

			xmlFragments = handler.getFragments();
			currentFragment = handler.getCurrentFragment();

			xmlFragments.add(currentFragment);
		}

		@Override
		public boolean canDoStacks() {
			return true;
		}

		@Override
		public int[] getPixelTypes(final String codec) {
			if (codec != null && (codec.equals("J2K") || codec.equals("JPEG"))) {
				return new int[] { FormatTools.INT8, FormatTools.UINT8 };
			}
			return super.getPixelTypes(codec);
		}

		@Override
		public void close() throws IOException {
			if (out != null) {
				out.writeBytes(xmlFragments.get(xmlFragments.size() - 1));
			}
			super.close();
			xmlFragments = null;
			service = null;
		}

		// -- Helper methods --

		/**
		 * Compress the given byte array using the current codec. The compressed
		 * data is then base64-encoded.
		 */
		private byte[] compress(byte[] b, final int imageIndex)
			throws FormatException, IOException
		{
			final MetadataRetrieve r = getMetadata().getOMEMeta().getRoot();
			final String type = r.getPixelsType(imageIndex).toString();
			final int pixelType = FormatTools.pixelTypeFromString(type);
			final int bytes = FormatTools.getBytesPerPixel(pixelType);

			final CodecOptions options = new CodecOptions();
			options.width = r.getPixelsSizeX(imageIndex).getValue().intValue();
			options.height = r.getPixelsSizeY(imageIndex).getValue().intValue();
			options.channels = 1;
			options.interleaved = false;
			options.signed = FormatTools.isSigned(pixelType);
			options.littleEndian =
				!r.getPixelsBinDataBigEndian(imageIndex, 0).booleanValue();
			options.bitsPerSample = bytes * 8;

			if (compression.equals("J2K")) {
				b = new JPEG2000Codec().compress(b, options);
			}
			else if (compression.equals("JPEG")) {
				b = new JPEGCodec().compress(b, options);
			}
			else if (compression.equals("zlib")) {
				b = new ZlibCodec().compress(b, options);
			}
			return new Base64Codec().compress(b, options);
		}

	}

	@Plugin(type = Translator.class, attrs = {
		@Attr(name = OMEXMLTranslator.SOURCE,
			value = io.scif.ome.xml.meta.OMEMetadata.CNAME),
		@Attr(name = OMEXMLTranslator.DEST, value = Metadata.CNAME) })
	public static class OMETranslator extends FromOMETranslator<Metadata> {

		@Override
		public void typedTranslate(final io.scif.ome.xml.meta.OMEMetadata source,
			final Metadata dest)
		{
			dest.setOMEMeta(source);
		}
	}

	@Plugin(type = Translator.class, attrs = {
		@Attr(name = OMEXMLTranslator.SOURCE, value = io.scif.Metadata.CNAME),
		@Attr(name = OMEXMLTranslator.DEST, value = Metadata.CNAME) },
		priority = Priority.LOW_PRIORITY)
	public static class OMEXMLTranslator extends
		AbstractTranslator<io.scif.Metadata, Metadata>
	{

		@Override
		public void typedTranslate(final io.scif.Metadata source,
			final Metadata dest)
		{
			final OMEXMLMetadata root = new OMEXMLMetadataImpl();
			final OMEMetadata meta = new OMEMetadata(getContext(), root);
			final OMEXMLMetadataService service =
				scifio().get(OMEXMLMetadataService.class);
			service.populatePixels(root, source);
			dest.setOMEMeta(meta);
		}
	}

	// -- Helper class --

	private static class OMEHandler extends BaseHandler {

		private final Vector<String> xmlFragments;
		private String currentFragment;

		// -- Constructor --

		public OMEHandler(final Vector<String> xmlFragments,
			final String currentFragment)
		{
			this.xmlFragments = xmlFragments;
			this.currentFragment = currentFragment;
		}

		// -- OMEHandler API methods --

		public Vector<String> getFragments() {
			return xmlFragments;
		}

		public String getCurrentFragment() {
			return currentFragment;
		}

		@Override
		public void characters(final char[] ch, final int start, final int length) {
			currentFragment += new String(ch, start, length);
		}

		@Override
		public void startElement(final String uri, final String localName,
			final String qName, final Attributes attributes)
		{
			final StringBuffer toAppend = new StringBuffer("\n<");
			toAppend.append(XMLTools.escapeXML(qName));
			for (int i = 0; i < attributes.getLength(); i++) {
				toAppend.append(" ");
				toAppend.append(XMLTools.escapeXML(attributes.getQName(i)));
				toAppend.append("=\"");
				toAppend.append(XMLTools.escapeXML(attributes.getValue(i)));
				toAppend.append("\"");
			}
			toAppend.append(">");
			currentFragment += toAppend.toString();
		}

		@Override
		public void endElement(final String uri, final String localName,
			final String qName)
		{
			if (qName.equals("Pixels")) {
				xmlFragments.add(currentFragment);
				currentFragment = "";
			}
			currentFragment += "</" + qName + ">";
		}

	}

	private static class OMEXMLHandler extends BaseHandler {

		private final StringBuffer xmlBuffer;
		private String currentQName;
		private Locator locator;
		private final Metadata meta;

		public OMEXMLHandler(final Metadata meta) {
			xmlBuffer = new StringBuffer();
			this.meta = meta;
		}

		@Override
		public void characters(final char[] ch, final int start, final int length) {
			if (currentQName.indexOf("BinData") < 0) {
				xmlBuffer.append(new String(ch, start, length));
			}
		}

		@Override
		public void endElement(final String uri, final String localName,
			final String qName)
		{
			xmlBuffer.append("</");
			xmlBuffer.append(qName);
			xmlBuffer.append(">");
		}

		@Override
		public void startElement(final String ur, final String localName,
			final String qName, final Attributes attributes)
		{
			currentQName = qName;

			if (qName.indexOf("BinData") == -1) {
				xmlBuffer.append("<");
				xmlBuffer.append(qName);
				for (int i = 0; i < attributes.getLength(); i++) {
					final String key = XMLTools.escapeXML(attributes.getQName(i));
					String value = XMLTools.escapeXML(attributes.getValue(i));
					if (key.equals("BigEndian")) {
						String endian = value.toLowerCase();
						if (!endian.equals("true") && !endian.equals("false")) {
							// hack for files that specify 't' or 'f' instead of
							// 'true' or 'false'
							if (endian.startsWith("t")) endian = "true";
							else if (endian.startsWith("f")) endian = "false";
						}
						value = endian;
					}
					xmlBuffer.append(" ");
					xmlBuffer.append(key);
					xmlBuffer.append("=\"");
					xmlBuffer.append(value);
					xmlBuffer.append("\"");
				}
				xmlBuffer.append(">");
			}
			else {
				meta.getBinData().add(
					new BinData(locator.getLineNumber(), locator.getColumnNumber()));
				final String compress = attributes.getValue("Compression");
				meta.getCompression().add(compress == null ? "" : compress);

				xmlBuffer.append("<");
				xmlBuffer.append(qName);
				for (int i = 0; i < attributes.getLength(); i++) {
					final String key = XMLTools.escapeXML(attributes.getQName(i));
					String value = XMLTools.escapeXML(attributes.getValue(i));
					if (key.equals("Length")) value = "0";
					xmlBuffer.append(" ");
					xmlBuffer.append(key);
					xmlBuffer.append("=\"");
					xmlBuffer.append(value);
					xmlBuffer.append("\"");
				}
				xmlBuffer.append(">");
			}
		}

		@Override
		public void endDocument() {
			meta.setOmexml(xmlBuffer.toString());
		}

		@Override
		public void setDocumentLocator(final Locator locator) {
			this.locator = locator;
		}
	}

	private static class BinData {

		private final int row;
		private final int column;

		public BinData(final int row, final int column) {
			this.row = row;
			this.column = column;
		}

		public int getRow() {
			return row;
		}

		public int getColumn() {
			return column;
		}
	}
}