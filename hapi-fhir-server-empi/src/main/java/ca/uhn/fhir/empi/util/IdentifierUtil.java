package ca.uhn.fhir.empi.util;

/*-
 * #%L
 * HAPI FHIR - Enterprise Master Patient Index
 * %%
 * Copyright (C) 2014 - 2021 University Health Network
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import org.hl7.fhir.instance.model.api.IBase;

public class IdentifierUtil {
	public static CanonicalIdentifier identifierDtFromIdentifier(IBase theIdentifier) {
		CanonicalIdentifier retval = new CanonicalIdentifier();

		// TODO add other fields like "use" etc
		if (theIdentifier instanceof org.hl7.fhir.dstu3.model.Identifier) {
			org.hl7.fhir.dstu3.model.Identifier ident = (org.hl7.fhir.dstu3.model.Identifier) theIdentifier;
			retval.setSystem(ident.getSystem()).setValue(ident.getValue());
		} else if (theIdentifier instanceof org.hl7.fhir.r4.model.Identifier) {
			org.hl7.fhir.r4.model.Identifier ident = (org.hl7.fhir.r4.model.Identifier) theIdentifier;
			retval.setSystem(ident.getSystem()).setValue(ident.getValue());
		} else if (theIdentifier instanceof org.hl7.fhir.r5.model.Identifier) {
			org.hl7.fhir.r5.model.Identifier ident = (org.hl7.fhir.r5.model.Identifier) theIdentifier;
			retval.setSystem(ident.getSystem()).setValue(ident.getValue());
		} else {
			throw new InternalErrorException("Expected 'Identifier' type but was '" + theIdentifier.getClass().getName() + "'");
		}
		return retval;
	}
}
