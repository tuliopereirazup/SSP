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
package org.jasig.ssp.factory.reference.impl;

import javax.validation.constraints.NotNull;

import org.jasig.ssp.dao.reference.EarlyAlertReasonDao;
import org.jasig.ssp.factory.reference.AbstractReferenceTOFactory;
import org.jasig.ssp.factory.reference.EarlyAlertReasonTOFactory;
import org.jasig.ssp.model.reference.EarlyAlertReason;
import org.jasig.ssp.service.ObjectNotFoundException;
import org.jasig.ssp.transferobject.reference.EarlyAlertReasonTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * EarlyAlertReason transfer object factory implementation class for converting
 * back and forth from EarlyAlertReason models.
 * 
 * @author jon.adams
 */
@Service
@Transactional(readOnly = true)
public class EarlyAlertReasonTOFactoryImpl extends
		AbstractReferenceTOFactory<EarlyAlertReasonTO, EarlyAlertReason>
		implements EarlyAlertReasonTOFactory {

	/**
	 * Constructor that initializes specific class instances for use by the
	 * common base class methods.
	 */
	public EarlyAlertReasonTOFactoryImpl() {
		super(EarlyAlertReasonTO.class, EarlyAlertReason.class);
	}

	@Autowired
	private transient EarlyAlertReasonDao dao;

	@Override
	protected EarlyAlertReasonDao getDao() {
		return dao;
	}

	@Override
	public EarlyAlertReason from(@NotNull final EarlyAlertReasonTO tObject)
			throws ObjectNotFoundException {
		final EarlyAlertReason model = super.from(tObject);

		model.setSortOrder(tObject.getSortOrder());

		return model;
	}
}
