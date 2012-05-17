package org.jasig.ssp.service.reference.impl; // NOPMD

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.jasig.ssp.dao.reference.JournalTrackDao;
import org.jasig.ssp.model.ObjectStatus;
import org.jasig.ssp.model.reference.JournalTrack;
import org.jasig.ssp.service.ObjectNotFoundException;
import org.jasig.ssp.util.sort.PagingWrapper;
import org.jasig.ssp.util.sort.SortingAndPaging;
import org.junit.Before;
import org.junit.Test;

public class JournalTrackServiceTest {

	private transient JournalTrackServiceImpl service;

	private transient JournalTrackDao dao;

	@Before
	public void setUp() {
		service = new JournalTrackServiceImpl();
		dao = createMock(JournalTrackDao.class);

		service.setDao(dao);
	}

	@Test
	public void testGetAll() {
		final List<JournalTrack> daoAll = new ArrayList<JournalTrack>();
		daoAll.add(new JournalTrack());

		expect(dao.getAll(isA(SortingAndPaging.class))).andReturn(
				new PagingWrapper<JournalTrack>(daoAll));

		replay(dao);

		final Collection<JournalTrack> all = service.getAll(
				new SortingAndPaging(ObjectStatus.ACTIVE)).getRows();
		assertFalse(all.isEmpty());
		verify(dao);
	}

	@Test
	public void testGet() throws ObjectNotFoundException {
		final UUID id = UUID.randomUUID();
		final JournalTrack daoOne = new JournalTrack(id);

		expect(dao.get(id)).andReturn(daoOne);

		replay(dao);

		assertNotNull(service.get(id));
		verify(dao);
	}

	@Test
	public void testSave() throws ObjectNotFoundException {
		final UUID id = UUID.randomUUID();
		final JournalTrack daoOne = new JournalTrack(id);

		expect(dao.save(daoOne)).andReturn(daoOne);

		replay(dao);

		assertNotNull(service.save(daoOne));
		verify(dao);
	}

	@Test
	public void testDelete() throws ObjectNotFoundException {
		final UUID id = UUID.randomUUID();
		final JournalTrack daoOne = new JournalTrack(id);

		expect(dao.get(id)).andReturn(daoOne);
		expect(dao.save(daoOne)).andReturn(daoOne);
		expect(dao.get(id)).andThrow(
				new ObjectNotFoundException(id, "JournalTrack"));

		replay(dao);

		service.delete(id);

		boolean found = true;
		try {
			service.get(id);
		} catch (final ObjectNotFoundException e) {
			found = false;
		}

		assertFalse(found);
		verify(dao);
	}

}
