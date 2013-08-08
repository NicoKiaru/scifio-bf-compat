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

package io.scif.ome.xml.translation;

import io.scif.Metadata;
import io.scif.ome.xml.meta.OMEMetadata;

/**
 * Abstract base class for all io.scif.Translators that translate to an
 * OMEMetadata object.
 * 
 * @author Mark Hiner
 */
public abstract class ToOMETranslator<M extends Metadata> extends
	OMETranslator<M, OMEMetadata>
{

	// -- Translator API Methods --

	public void translate(final M source, final OMEMetadata destination) {
		super.translate(source, destination);
	}
}
