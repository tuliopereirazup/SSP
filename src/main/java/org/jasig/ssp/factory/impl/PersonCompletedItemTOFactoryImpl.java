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
package org.jasig.ssp.factory.impl;

import org.jasig.ssp.dao.PersonCompletedItemDao;
import org.jasig.ssp.factory.AbstractAuditableTOFactory;
import org.jasig.ssp.factory.PersonCompletedItemTOFactory;
import org.jasig.ssp.model.PersonCompletedItem;
import org.jasig.ssp.service.ObjectNotFoundException;
import org.jasig.ssp.service.PersonService;
import org.jasig.ssp.service.reference.CompletedItemService;
import org.jasig.ssp.transferobject.PersonCompletedItemTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PersonCompletedItemTOFactoryImpl extends
		AbstractAuditableTOFactory<PersonCompletedItemTO, PersonCompletedItem>
		implements PersonCompletedItemTOFactory {

	public PersonCompletedItemTOFactoryImpl() {
		super(PersonCompletedItemTO.class, PersonCompletedItem.class);
	}

	@Autowired
	private transient PersonCompletedItemDao dao;

	@Autowired
	private transient CompletedItemService completedItemsService;

	@Autowired
	private transient PersonService personService;

	@Override
	protected PersonCompletedItemDao getDao() {
		return dao;
	}

	@Override
	public PersonCompletedItem from(final PersonCompletedItemTO tObject)
			throws ObjectNotFoundException {
		final PersonCompletedItem model = super.from(tObject);
		model.setPerson(tObject.getPersonId() == null ? null :
				personService.get(tObject.getPersonId()));
		model.setCompletedItems(tObject.getCompletedItemId() == null ? null :
			completedItemsService.get(tObject.getCompletedItemId()));

		return model;
	}
}