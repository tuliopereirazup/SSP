package org.studentsuccessplan.ssp.service.reference.impl;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.studentsuccessplan.ssp.dao.reference.SelfHelpGuideDao;
import org.studentsuccessplan.ssp.model.ObjectStatus;
import org.studentsuccessplan.ssp.model.reference.SelfHelpGuide;
import org.studentsuccessplan.ssp.service.ObjectNotFoundException;
import org.studentsuccessplan.ssp.service.SecurityService;
import org.studentsuccessplan.ssp.util.sort.SortingAndPaging;

/**
 * Self-Help Guide service tests
 */
public class SelfHelpGuideServiceTest {

	private transient SelfHelpGuideServiceImpl service;

	private transient SelfHelpGuideDao dao;

	private transient SecurityService securityService;

	@Before
	public void setUp() {
		dao = createMock(SelfHelpGuideDao.class);
		securityService = createMock(SecurityService.class);
		service = new SelfHelpGuideServiceImpl(dao, securityService);
	}

	@Test
	public void testGetAllNotAuthenticated() {
		final List<SelfHelpGuide> daoAll = new ArrayList<SelfHelpGuide>();
		daoAll.add(new SelfHelpGuide());

		expect(securityService.isAuthenticated()).andReturn(false);
		expect(dao.findAllActiveForUnauthenticated()).andReturn(daoAll);

		replay(dao);
		replay(securityService);

		final List<SelfHelpGuide> all = service.getAll(new SortingAndPaging(
				ObjectStatus.ACTIVE));
		assertFalse(all.isEmpty());
		verify(dao);
		verify(securityService);
	}

	@Test
	public void testGetAllAuthenticated() {
		final List<SelfHelpGuide> daoAll = new ArrayList<SelfHelpGuide>();
		daoAll.add(new SelfHelpGuide());

		expect(securityService.isAuthenticated()).andReturn(true);
		expect(dao.getAll(isA(SortingAndPaging.class))).andReturn(daoAll);

		replay(dao);
		replay(securityService);

		final List<SelfHelpGuide> all = service.getAll(new SortingAndPaging(
				ObjectStatus.ACTIVE));
		assertFalse(all.isEmpty());
		verify(dao);
		verify(securityService);
	}

	@Test
	public void testGet() throws ObjectNotFoundException {
		final UUID id = UUID.randomUUID();
		final SelfHelpGuide daoOne = new SelfHelpGuide(id);

		expect(dao.get(id)).andReturn(daoOne);

		replay(dao);

		assertNotNull(service.get(id));
		verify(dao);
	}

	@Test
	public void testSave() throws ObjectNotFoundException {
		final UUID id = UUID.randomUUID();
		final SelfHelpGuide daoOne = new SelfHelpGuide(id);

		expect(dao.get(id)).andReturn(daoOne);
		expect(dao.save(daoOne)).andReturn(daoOne);

		replay(dao);

		assertNotNull(service.save(daoOne));
		verify(dao);
	}

	@Test
	public void testDelete() throws ObjectNotFoundException {
		final UUID id = UUID.randomUUID();
		final SelfHelpGuide daoOne = new SelfHelpGuide(id);

		expect(dao.get(id)).andReturn(daoOne).times(2);
		expect(dao.save(daoOne)).andReturn(daoOne);
		expect(dao.get(id)).andReturn(null);

		replay(dao);

		service.delete(id);

		boolean found = true;
		try {
			service.get(id);
		} catch (ObjectNotFoundException e) {
			found = false;
		}

		assertFalse(found);
		verify(dao);
	}

}
