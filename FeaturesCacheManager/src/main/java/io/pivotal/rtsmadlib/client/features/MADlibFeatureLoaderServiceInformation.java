/**********************************************************************************************
   Copyright 2019 Pivotal Software

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 *********************************************************************************************/
package io.pivotal.rtsmadlib.client.features;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.info.Info.Builder;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;

import io.pivotal.rtsmadlib.client.features.cache.CacheLoader;
import io.pivotal.rtsmadlib.client.features.meta.ApplicationProperties;

/**
 * @author Sridhar Paladugu
 *
 */
@Component
public class MADlibFeatureLoaderServiceInformation implements InfoContributor {

	@Autowired
	CacheLoader cacheLoader;
	@Autowired
	ApplicationProperties props;
	static final Log log = LogFactory.getLog(MADlibFeatureLoaderServiceInformation.class.getName());

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.boot.actuate.info.InfoContributor#contribute(org.
	 * springframework.boot.actuate.info.Info.Builder)
	 */
	@Override
	public void contribute(Builder builder) {
		Long keys = -1l;
		try {
			keys = cacheLoader.keyCount();
		} catch (Exception e) {
			log.error(e);
		}
		builder.withDetail("Feature Cache - ", props.getFeatureName());
		if (null != props.getFeatureFunctions()) {
			props.getFeatureFunctions().forEach(featureFunction -> {
				builder.withDetail("Feature Function - ", featureFunction);
			});
		}
		builder.withDetail("Feature source Schema - ", props.getFeatureSourceSchema());
		props.getFeatureSourceTables().keySet().forEach(table -> {
			builder.withDetail("Feature source table - ", table + " :: " + props.getFeatureSourceTables().get(table));
		});
		builder.withDetail("Feature keys size - ", keys);
	}

}
