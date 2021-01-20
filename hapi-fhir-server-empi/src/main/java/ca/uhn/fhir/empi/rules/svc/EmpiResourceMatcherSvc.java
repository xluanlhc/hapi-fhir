package ca.uhn.fhir.empi.rules.svc;

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

import ca.uhn.fhir.context.ConfigurationException;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.empi.api.EmpiMatchEvaluation;
import ca.uhn.fhir.empi.api.EmpiMatchOutcome;
import ca.uhn.fhir.empi.api.EmpiMatchResultEnum;
import ca.uhn.fhir.empi.api.IEmpiSettings;
import ca.uhn.fhir.empi.log.Logs;
import ca.uhn.fhir.empi.rules.json.EmpiFieldMatchJson;
import ca.uhn.fhir.empi.rules.json.EmpiRulesJson;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

/**
 * The EmpiResourceComparator is in charge of performing actual comparisons between left and right records.
 * It does so by calling individual comparators, and returning a vector based on the combination of
 * field comparators that matched.
 */

@Service
public class EmpiResourceMatcherSvc {
	private static final Logger ourLog = Logs.getEmpiTroubleshootingLog();

	private final FhirContext myFhirContext;
	private final IEmpiSettings myEmpiSettings;
	private EmpiRulesJson myEmpiRulesJson;
	private final List<EmpiResourceFieldMatcher> myFieldMatchers = new ArrayList<>();

	@Autowired
	public EmpiResourceMatcherSvc(FhirContext theFhirContext, IEmpiSettings theEmpiSettings) {
		myFhirContext = theFhirContext;
		myEmpiSettings = theEmpiSettings;
	}

	@PostConstruct
	public void init() {
		myEmpiRulesJson = myEmpiSettings.getEmpiRules();
		if (myEmpiRulesJson == null) {
			throw new ConfigurationException("Failed to load EMPI Rules.  If EMPI is enabled, then EMPI rules must be available in context.");
		}
		for (EmpiFieldMatchJson matchFieldJson : myEmpiRulesJson.getMatchFields()) {
			myFieldMatchers.add(new EmpiResourceFieldMatcher(myFhirContext, matchFieldJson));
		}

	}

	/**
	 * Given two {@link IBaseResource}s, perform all comparisons on them to determine an {@link EmpiMatchResultEnum}, indicating
	 * to what level the two resources are considered to be matching.
	 *
	 * @param theLeftResource The first {@link IBaseResource}.
	 * @param theRightResource The second {@link IBaseResource}
	 *
	 * @return an {@link EmpiMatchResultEnum} indicating the result of the comparison.
	 */
	public EmpiMatchOutcome getMatchResult(IBaseResource theLeftResource, IBaseResource theRightResource) {
		return match(theLeftResource, theRightResource);
	}

	EmpiMatchOutcome match(IBaseResource theLeftResource, IBaseResource theRightResource) {
		EmpiMatchOutcome matchResult = getMatchOutcome(theLeftResource, theRightResource);
		EmpiMatchResultEnum matchResultEnum = myEmpiRulesJson.getMatchResult(matchResult.vector);
		matchResult.setMatchResultEnum(matchResultEnum);
		if (ourLog.isDebugEnabled()) {
			if (matchResult.isMatch() || matchResult.isPossibleMatch()) {
				ourLog.debug("{} {} with field matchers {}", matchResult, theRightResource.getIdElement().toUnqualifiedVersionless(), myEmpiRulesJson.getFieldMatchNamesForVector(matchResult.vector));
			} else if (ourLog.isTraceEnabled()) {
				ourLog.trace("{} {}.  Field matcher results: {}", matchResult, theRightResource.getIdElement().toUnqualifiedVersionless(), myEmpiRulesJson.getDetailedFieldMatchResultForUnmatchedVector(matchResult.vector));
			}
		}
		return matchResult;
	}

	/**
	 * This function generates a `match vector`, which is a long representation of a binary string
	 * generated by the results of each of the given comparator matches. For example.
	 * start with a binary representation of the value 0 for long: 0000
	 * first_name matches, so the value `1` is bitwise-ORed to the current value (0) in right-most position.
	 * `0001`
	 *
	 * Next, we look at the second field comparator, and see if it matches. If it does, we left-shift 1 by the index
	 * of the comparator, in this case also 1.
	 * `0010`
	 *
	 * Then, we bitwise-or it with the current retval:
	 * 0001|0010 = 0011
	 * The binary string is now `0011`, which when you return it as a long becomes `3`.
	 */
	private EmpiMatchOutcome getMatchOutcome(IBaseResource theLeftResource, IBaseResource theRightResource) {
		long vector = 0;
		double score = 0.0;
		for (int i = 0; i < myFieldMatchers.size(); ++i) {
			//any that are not for the resourceType in question.
			EmpiResourceFieldMatcher fieldComparator = myFieldMatchers.get(i);
			EmpiMatchEvaluation matchEvaluation = fieldComparator.match(theLeftResource, theRightResource);
			if (matchEvaluation.match) {
				vector |= (1 << i);
			}
			score += matchEvaluation.score;
		}

		EmpiMatchOutcome retVal = new EmpiMatchOutcome(vector, score);
		retVal.setEmpiRuleCount(myFieldMatchers.size());
		return retVal;
	}
}
