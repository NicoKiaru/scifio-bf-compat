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

package io.scif.ome.xml.services;

import io.scif.SCIFIOService;
import io.scif.ome.xml.meta.OMEMetadata;
import io.scif.ome.xml.meta.OMEXMLMetadata;
import io.scif.services.ServiceException;

import java.util.Hashtable;

import loci.formats.meta.MetadataRetrieve;
import loci.formats.meta.MetadataStore;

/**
 * <dl>
 * <dt><b>Source code:</b></dt>
 * <dd><a href=
 * "http://trac.openmicroscopy.org.uk/ome/browser/bioformats.git/components/bio-formats/src/loci/formats/services/OMEXMLService.java"
 * >Trac</a>, <a href=
 * "http://git.openmicroscopy.org/?p=bioformats.git;a=blob;f=components/bio-formats/src/loci/formats/services/OMEXMLService.java;hb=HEAD"
 * >Gitweb</a></dd>
 * </dl>
 */
public interface OMEXMLService extends SCIFIOService {

	/**
	 * Retrieves the latest supported version of the OME-XML schema.
	 */
	public String getLatestVersion();

	/**
	 * Transforms the given OME-XML string to the latest supported version of of
	 * the OME-XML schema.
	 */
	public String transformToLatestVersion(String xml);

	/**
	 * Creates an OME-XML metadata object using reflection, to avoid direct
	 * dependencies on the optional {@link loci.formats.ome} package.
	 * 
	 * @return A new instance of {@link loci.formats.ome.AbstractOMEXMLMetadata},
	 *         or null if the class is not available.
	 * @throws ServiceException If there is an error creating the OME-XML metadata
	 *           object.
	 */
	public OMEXMLMetadata createOMEXMLMetadata() throws ServiceException;

	/**
	 * Creates an OME-XML metadata object using reflection, to avoid direct
	 * dependencies on the optional {@link loci.formats.ome} package, wrapping a
	 * DOM representation of the given OME-XML string.
	 * 
	 * @return A new instance of {@link loci.formats.ome.AbstractOMEXMLMetadata},
	 *         or null if the class is not available.
	 * @throws ServiceException If there is an error creating the OME-XML metadata
	 *           object.
	 */
	public OMEXMLMetadata createOMEXMLMetadata(String xml)
		throws ServiceException;

	/**
	 * Creates an OME-XML metadata object using reflection, to avoid direct
	 * dependencies on the optional {@link loci.formats.ome} package, wrapping a
	 * DOM representation of the given OME-XML string.
	 * 
	 * @param xml The OME-XML string to use for initial population of the metadata
	 *          object.
	 * @param version The OME-XML version to use (e.g., "2003-FC" or "2007-06").
	 *          If the xml and version parameters are both null, the newest
	 *          version is used.
	 * @return A new instance of {@link loci.formats.ome.AbstractOMEXMLMetadata},
	 *         or null if the class is not available.
	 * @throws ServiceException If there is an error creating the OME-XML metadata
	 *           object.
	 */
	public OMEXMLMetadata createOMEXMLMetadata(String xml, String version)
		throws ServiceException;

	/**
	 * Constructs an OME root node.
	 * 
	 * @param xml String of XML to create the root node from.
	 * @return An ome.xml.OMEXMLNode subclass root node.
	 * @throws ServiceException If there is an error creating the OME-XML metadata
	 *           object.
	 */
	public Object createOMEXMLRoot(String xml) throws ServiceException;

	/**
	 * Checks whether the given object is an OME-XML metadata object.
	 * 
	 * @return True if the object is an instance of
	 *         {@link loci.formats.ome.AbstractOMEXMLMetadata}.
	 */
	public boolean isOMEXMLMetadata(Object o);

	/**
	 * Checks whether the given object is an OME-XML root object.
	 * 
	 * @return True if the object is an instance of {@link ome.xml.OMEXMLNode}.
	 */
	public boolean isOMEXMLRoot(Object o);

	/**
	 * Gets the schema version for the given OME-XML metadata or root object
	 * (e.g., "2007-06" or "2003-FC").
	 * 
	 * @return OME-XML schema version, or null if the object is not an instance of
	 *         {@link loci.formats.ome.OMEXMLMetadata} or ome.xml.OMEXMLNode.
	 */
	public String getOMEXMLVersion(Object o);

	/**
	 * Returns a {@link loci.formats.ome.OMEXMLMetadata} object with the same
	 * contents as the given MetadataRetrieve, converting it if necessary.
	 * 
	 * @throws ServiceException If there is an error creating the OME-XML metadata
	 *           object.
	 */
	public OMEXMLMetadata getOMEMetadata(MetadataRetrieve src)
		throws ServiceException;

	/**
	 * Extracts an OME-XML metadata string from the given metadata object, by
	 * converting to an OME-XML metadata object if necessary.
	 * 
	 * @throws ServiceException If there is an error creating the OME-XML metadata
	 *           object.
	 */
	public String getOMEXML(MetadataRetrieve src) throws ServiceException;

	/**
	 * Attempts to validate the given OME-XML string using Java's XML validation
	 * facility. Requires Java 1.5+.
	 * 
	 * @param xml XML string to validate.
	 * @return true if the XML successfully validates.
	 */
	public boolean validateOMEXML(String xml);

	/**
	 * Attempts to validate the given OME-XML string using Java's XML validation
	 * facility. Requires Java 1.5+.
	 * 
	 * @param xml XML string to validate.
	 * @param pixelsHack Whether to ignore validation errors due to childless
	 *          Pixels elements
	 * @return true if the XML successfully validates.
	 */
	public boolean validateOMEXML(String xml, boolean pixelsHack);

	/**
	 * Adds the key/value pairs in the specified Hashtable as new OriginalMetadata
	 * annotations in the given OME-XML metadata object.
	 * 
	 * @param omexmlMeta An object of type {@link loci.formats.ome.OMEXMLMetadata}
	 *          .
	 * @param metadata A hashtable containing metadata key/value pairs.
	 */
	public void populateOriginalMetadata(OMEMetadata omexmlMeta,
		Hashtable<String, Object> metadata);

	/**
	 * Adds the specified key/value pair as a new OriginalMetadata node to the
	 * given OME-XML metadata object.
	 * 
	 * @param omexmlMeta An object of type {@link loci.formats.ome.OMEXMLMetadata}
	 *          .
	 * @param key Metadata key to populate.
	 * @param value Metadata value corresponding to the specified key.
	 */
	public void populateOriginalMetadata(OMEMetadata omexmlMeta, String key,
		String value);

	/**
	 * Parse any OriginalMetadata annotations from the given OME-XML metadata
	 * object and store them in a Hashtable.
	 * 
	 * @param omexmlMeta An object of type {@link loci.formats.ome.OMEXMLMetadata}
	 *          .
	 */
	public Hashtable<String, Object>
		getOriginalMetadata(OMEXMLMetadata omexmlMeta);

	/**
	 * Converts information from an OME-XML string (source) into a metadata store
	 * (destination).
	 * 
	 * @throws ServiceException If there is an error creating the OME-XML metadata
	 *           object.
	 */
	public void convertMetadata(String xml, MetadataStore dest)
		throws ServiceException;

	/**
	 * Copies information from a metadata retrieval object (source) into a
	 * metadata store (destination).
	 */
	public void convertMetadata(MetadataRetrieve src, MetadataStore dest);

	/**
	 * Remove all of the BinData elements from the given OME-XML metadata object.
	 */
	public void removeBinData(OMEXMLMetadata omexmlMeta);

	/**
	 * Remove all but the first sizeC valid Channel elements from the given
	 * OME-XML metadata object.
	 */
	public void removeChannels(OMEXMLMetadata omexmlMeta, int image, int sizeC);

	/**
	 * Insert a MetadataOnly element under the Image specified by 'index' in the
	 * given OME-XML metadata object.
	 */
	public void addMetadataOnly(OMEXMLMetadata omexmlMeta, int image);

	/**
	 * Determine whether or not two OMEXMLMetadata objects are equal. Equality is
	 * defined as: * having the same object graph (without regard to specific ID
	 * values) * having the exact same attribute values on every node (with the
	 * exception of 'ID' attributes) Note that StructuredAnnotations are ignored,
	 * i.e. the two OMEXMLMetadata objects may have wildly different things under
	 * StructuredAnnotations and still be considered equal.
	 */
	public boolean isEqual(OMEXMLMetadata src1, OMEXMLMetadata src2);

	// -- Utility methods - casting --

	/**
	 * Gets the given {@link MetadataRetrieve} object as a {@link MetadataStore}.
	 * Returns null if the object is incompatible and cannot be casted.
	 */
	public MetadataStore asStore(MetadataRetrieve meta);

	/**
	 * Gets the given {@link MetadataStore} object as a {@link MetadataRetrieve}.
	 * Returns null if the object is incompatible and cannot be casted.
	 */
	public MetadataRetrieve asRetrieve(MetadataStore meta);

}
