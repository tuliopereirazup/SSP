/**
 * Licensed to Apereo under one or more contributor license
 * agreements. See the NOTICE file distributed with this work
 * for additional information regarding copyright ownership.
 * Apereo licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License.  You may obtain a
 * copy of the License at the following location:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jasig.ssp.portlet.ssp;

import java.util.HashMap;
import java.util.Map;

import org.jasig.ssp.service.ServerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.portlet.ModelAndView;
import org.springframework.web.portlet.bind.annotation.RenderMapping;

@Controller
@RequestMapping("VIEW")
public final class SspPortletController {
	
	@Value("#{configProperties.ssp_main_use_minifed_js}")
	private boolean sspMainUseMinifiedJs = false;
	
	@Value("#{configProperties.ssp_set_develop_mode_on}")
	private boolean developModeOn = false;
	
	@Autowired
	private ServerService serverService;
	
	@RenderMapping
	public ModelAndView show(){
		Map<String,Object> model = new HashMap<String,Object>();
		model.put("useMinified", sspMainUseMinifiedJs);
		model.put("developModeOn", developModeOn);

		try{
			String build = "/versioned/" + serverService.getVersionProfile().get("buildDate").toString();
			model.put("cachebust", build);
		}catch(Exception exp){
			
		}
		return new ModelAndView("ssp-main", "model", model);
	}

}
