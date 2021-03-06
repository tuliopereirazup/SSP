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
package org.jasig.ssp.service.impl; // NOPMD

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.mail.SendFailedException;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang.StringUtils;
import org.jasig.ssp.dao.TaskDao;
import org.jasig.ssp.factory.TaskTOFactory;
import org.jasig.ssp.model.Goal;
import org.jasig.ssp.model.Message;
import org.jasig.ssp.model.ObjectStatus;
import org.jasig.ssp.model.Person;
import org.jasig.ssp.model.Strength;
import org.jasig.ssp.model.SubjectAndBody;
import org.jasig.ssp.model.Task;
import org.jasig.ssp.model.TaskMessageEnqueue;
import org.jasig.ssp.model.reference.Challenge;
import org.jasig.ssp.model.reference.ChallengeReferral;
import org.jasig.ssp.model.reference.ConfidentialityLevel;
import org.jasig.ssp.security.SspUser;
import org.jasig.ssp.service.AbstractRestrictedPersonAssocAuditableService;
import org.jasig.ssp.service.MessageService;
import org.jasig.ssp.service.ObjectNotFoundException;
import org.jasig.ssp.service.PersonService;
import org.jasig.ssp.service.TaskMessageEnqueueService;
import org.jasig.ssp.service.TaskService;
import org.jasig.ssp.service.reference.ConfidentialityLevelService;
import org.jasig.ssp.service.reference.ConfigService;
import org.jasig.ssp.service.reference.MessageTemplateService;
import org.jasig.ssp.transferobject.GoalTO;
import org.jasig.ssp.transferobject.StrengthTO;
import org.jasig.ssp.transferobject.TaskTO;
import org.jasig.ssp.transferobject.form.EmailAddress;
import org.jasig.ssp.transferobject.form.EmailPersonTasksForm;
import org.jasig.ssp.transferobject.form.EmailStudentRequestForm;
import org.jasig.ssp.transferobject.messagetemplate.TaskMessageTemplateTO;
import org.jasig.ssp.transferobject.reports.EntityCountByCoachSearchForm;
import org.jasig.ssp.transferobject.reports.EntityStudentCountByCoachTO;
import org.jasig.ssp.util.DateTimeUtils;
import org.jasig.ssp.util.collections.Pair;
import org.jasig.ssp.util.sort.PagingWrapper;
import org.jasig.ssp.util.sort.SortingAndPaging;
import org.jasig.ssp.web.api.validation.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

@Service
@Transactional
public class TaskServiceImpl
		extends AbstractRestrictedPersonAssocAuditableService<Task>
		implements TaskService {

	@Autowired
	private transient TaskDao dao;
	
	@Autowired
	private transient TaskTOFactory factory;

	@Autowired
	private transient MessageService messageService;
	
	@Autowired
	private transient PersonService personService;

	@Autowired
	private transient MessageTemplateService messageTemplateService;
	
	@Autowired
	private transient TaskMessageEnqueueService taskMessageSentService;

	@Autowired
	private transient ConfidentialityLevelService confidentialityLevelService;

	@Autowired
	private transient ConfigService configService;

	private static final Logger LOGGER = LoggerFactory
			.getLogger(TaskServiceImpl.class);

	@Override
	protected TaskDao getDao() {
		return dao;
	}

	private List<Integer> getNumberOfDaysPriorForTaskReminder() {
		final String numVal = configService
				.getByNameNull("numberOfDaysPriorForTaskReminders");
		if(StringUtils.isBlank(numVal))
			return Lists.newArrayList(new Integer(14));
		String[] numVals = numVal.split(",");
		List<Integer>daysPrior = new ArrayList<Integer>();
		
		for(String str:numVals){
			if (!StringUtils.isBlank(str) && StringUtils.isNumeric(str)) {
				daysPrior.add(Integer.parseInt(str));
			}
		}
		if(daysPrior.size() == 0){
			daysPrior.add(14);
		}
		Collections.sort(daysPrior);
		return daysPrior;
	}

	@Override
	public Task save(final Task obj) throws ObjectNotFoundException {
		return getDao().save(obj);
	}

	@Override
	public List<Task> getAllForPerson(final Person person,
			final boolean complete,
			final SspUser requester,
			final SortingAndPaging sAndP) {
		return getDao().getAllForPersonId(person.getId(), complete,
				requester, sAndP);
	}

	@Override
	public List<Task> getAllForSessionId(final String sessionId,
			final SortingAndPaging sAndP) {
		return getDao().getAllForSessionId(sessionId, sAndP);
	}

	@Override
	public List<Task> getAllForSessionId(final String sessionId,
			final boolean complete,
			final SortingAndPaging sAndP) {
		return getDao().getAllForSessionId(sessionId, complete, sAndP);
	}

	@Override
	public List<Task> getAllWhichNeedRemindersSent(final SortingAndPaging sAndP) {
		return getDao().getAllWhichNeedRemindersSent(sAndP);
	}

	@Override
	public void markTaskComplete(final Task task) {
		task.setCompletedDate(new Date());
		getDao().save(task);
	}

	@Override
	public void markTaskIncomplete(final Task task) {
		task.setCompletedDate(null);
		getDao().save(task);
	}

	@Override
	public void markTaskCompletion(final Task task, final boolean complete) {
		if (complete) {
			markTaskComplete(task);
		} else {
			markTaskIncomplete(task);
		}
	}

	@Override
	public void setReminderSentDateToToday(final Task task) {
		task.setReminderSentDate(new Date());
		getDao().save(task);
	}

	@Override
	public List<Task> getAllForPersonAndChallengeReferral(final Person person,
			final boolean complete, final ChallengeReferral challengeReferral,
			final SspUser requester,
			final SortingAndPaging sAndP) {
		return dao.getAllForPersonIdAndChallengeReferralId(person.getId(),
				complete, challengeReferral.getId(),
				requester, sAndP);
	}

	@Override
	public List<Task> getAllForSessionIdAndChallengeReferral(
			final String sessionId, final boolean complete,
			final ChallengeReferral challengeReferral,
			final SortingAndPaging sAndP) {
		return dao.getAllForSessionIdAndChallengeReferralId(sessionId,
				complete, challengeReferral.getId(), sAndP);
	}

	@Override
	public Map<String, List<Task>> getAllGroupedByTaskGroup(
			final Person person,
			final SspUser requester,
			final SortingAndPaging sAndP) {

		final Map<String, List<Task>> grouped = Maps.newTreeMap();
		final PagingWrapper<Task> tasksForPerson = dao
				.getAllForPersonId(person.getId(), requester,
						sAndP);

		for (final Task task : tasksForPerson.getRows()) {
			final String group = task.getGroup();
			List<Task> tasksForGroup;
			if (grouped.keySet().contains(group)) {
				tasksForGroup = grouped.get(group);
			} else {
				tasksForGroup = new ArrayList<Task>(); // NOPMD by jon.adams
				grouped.put(group, tasksForGroup);
			}

			tasksForGroup.add(task);
		}

		return grouped;
	}

	@Override
	public Task createForPersonWithChallengeReferral(final Challenge challenge,
			final ChallengeReferral challengeReferral, final Person person,
			final String sessionId) throws ObjectNotFoundException,
			ValidationException {

		// Create, fill, and persist a new Task
		final Task task = new Task();

		task.setChallenge(challenge);
		task.setChallengeReferral(challengeReferral);
		task.setPerson(person);
		task.setSessionId(sessionId);
		task.setDescription(challengeReferral.getPublicDescription());
		task.setName(challengeReferral.getName());
		task.setLink(challengeReferral.getLink());

		setDefaultConfidentialityLevel(task, challenge);

		create(task);

		return task;
	}

	private void setDefaultConfidentialityLevel(final Task task,
			final Challenge challenge) {
		if (challenge != null
				&& challenge.getDefaultConfidentialityLevel() != null) {
			task.setConfidentialityLevel(challenge
					.getDefaultConfidentialityLevel());
		}

		if (task.getConfidentialityLevel() == null) {
			try {
				task.setConfidentialityLevel(confidentialityLevelService
						.get(ConfidentialityLevel.CONFIDENTIALITYLEVEL_EVERYONE));
			} catch (final ObjectNotFoundException e) {
				LOGGER.error(
						"Unable to find the default confidentiality level", e);
			}
		}

	}

	@Override
	public Task createCustomTaskForPerson(final String name,
			final String description,
			final Person student,
			final String sessionId)
			throws ObjectNotFoundException, ValidationException {
		final Task customTask = new Task();
		customTask.setDescription(description);
		customTask.setPerson(student);
		customTask.setName(name);
		setDefaultConfidentialityLevel(customTask, null);

		create(customTask);

		return customTask;
	}

	@Override
	public void sendNoticeToStudentOnCustomTask(final Task customTask)
			throws ObjectNotFoundException,
			SendFailedException, ValidationException {

		final SubjectAndBody subjAndBody = messageTemplateService
				.createStudentIntakeTaskMessage(customTask);

		messageService.createMessage(customTask.getPerson(), null, subjAndBody);
	}

	@Override
	public void sendTasksForPersonToEmail(@NotNull final List<Task> tasks,
			final List<Goal> goals, final List<Strength> strengths, final Person student,
			final EmailPersonTasksForm form)
			throws ObjectNotFoundException, ValidationException {
		EmailAddress addresses;
		try {
			addresses = form.getValidDeliveryAddressesOrFail(true);
		} catch ( ValidationException e ) {
			// Historically this method has always thrown unchecked exceptions for this sort
			// of validation failure, and switching to a checked exception is likely to
			// disrupt transaction management, so converted checked to unchecked here
			throw new IllegalArgumentException("Must enter at least one email address", e);
		}
		final List<TaskTO> taskTOs = TaskTO.toTOList(tasks);
		final List<GoalTO> goalTOs = GoalTO.toTOList(goals);
		final List<StrengthTO> strengthTOs = StrengthTO.toTOList(strengths);

		final SubjectAndBody subjAndBody = messageTemplateService
				.createActionPlanMessage(student, taskTOs, goalTOs, strengthTOs);
		Set<String> ccAddresses = new HashSet<String>();

		if(form.getCoachEmail() != null && !StringUtils.isBlank(form.getCoachEmail()))
		{
			ccAddresses.addAll(student.getWatcherEmailAddresses());
		}
		if(!StringUtils.isBlank(addresses.getCc()))
		{
			ccAddresses.addAll(org.springframework.util.StringUtils.commaDelimitedListToSet(addresses.getCc()));
		}

		messageService.createMessage(addresses.getTo(),  org.springframework.util.StringUtils.arrayToCommaDelimitedString(ccAddresses
				.toArray(new String[ccAddresses.size()])), subjAndBody);
	}
	
	@Override
	public void sendTasksForPersonToEmail(@NotNull final List<Task> tasks,
			final Person student,
			final String emailAdresses)
			throws ObjectNotFoundException, ValidationException {
		if(!EmailStudentRequestForm.hasValidEmailAddress(emailAdresses, true)){
			throw new IllegalArgumentException("Must enter at least one email address");
		}
		final List<TaskTO> taskTOs = TaskTO.toTOList(tasks);

		final SubjectAndBody subjAndBody = messageTemplateService
				.createActionPlanMessage(student, taskTOs, null, null);

		List<String> watcherEmailAddresses = student.getWatcherEmailAddresses();
		
		messageService.createMessage(emailAdresses, org.springframework.util.StringUtils.arrayToCommaDelimitedString(watcherEmailAddresses
				.toArray(new String[watcherEmailAddresses.size()])), subjAndBody);
	}
	
	

	@Override
	public List<Task> getTasksForPersonIfNoneSelected(
			final List<UUID> selectedIds, final Person person,
			final SspUser requester, final String sessionId,
			final SortingAndPaging sAndP) {
		/*
		 * If tasks are selected, get them, otherwise return the tasks for the
		 * person, (just for the session if it is the anon user).
		 */
		if (selectedIds != null && !selectedIds.isEmpty()) {
			return get(selectedIds, requester, sAndP);
		}

		if (person.getId() == SspUser.ANONYMOUS_PERSON_ID) {
			return getAllForSessionId(sessionId, sAndP);
		}

		return (List<Task>) getAllForPerson(person, requester, sAndP).getRows();
	}

	/**
	 * Used by ScheduledTaskWrapperService default 2 a.m.
	 * Sends message of overdue tasks to students.
	 */
	@Override
	public void sendAllTaskReminderNotifications() {

		if ( Thread.currentThread().isInterrupted() ) {
			LOGGER.info("Abandoning sendAllTaskReminderNotifications because of thread interruption");
			return;
		}

		final SortingAndPaging sAndP = new SortingAndPaging(
				ObjectStatus.ACTIVE);

		LOGGER.info("BEGIN : sendTaskReminderNotifications()");

		try {

			// Calculate reminder window start date
			final Integer now = DateTimeUtils.daysSince1900(new Date());

			final List<Task> tasks = getAllWhichNeedRemindersSent(sAndP);

			for (final Task task : tasks) {

				if ( Thread.currentThread().isInterrupted() ) {
					LOGGER.info("Abandoning sendAllTaskReminderNotifications because of thread interruption");
					break;
				}
				Integer dueDate = DateTimeUtils.daysSince1900(task.getDueDate());
				
				for(Integer daysBefore:getNumberOfDaysPriorForTaskReminder()){
					if(daysBefore.equals(dueDate - now) && !messageSent(task, daysBefore)) {
						sendReminderMessage(task, daysBefore);
						break;
					}
				}				
			}

		} catch (final Exception e) {
			LOGGER.error("ERROR : sendTaskReminderNotifications() : {}",
					e.getMessage(), e);
		}

		LOGGER.info("END : sendTaskReminderNotifications()");
	}
	
	private Boolean messageSent(Task task, Integer daysBefore){
		if(task.getMessagesSent() != null && !task.getMessagesSent().isEmpty()){
			for(TaskMessageEnqueue messageSent:task.getMessagesSent()){
				if(task.getDueDate().equals(messageSent.getTaskDueDate()) 
						&& messageSent.getMessageEnqueueDate() != null 
						&& messageSent.getDaysBefore().equals(daysBefore))
					return true;
			}
		}
		return false;
	}
	
	private void sendReminderMessage(final Task task, final Integer daysBefore) throws SendFailedException, 
		ObjectNotFoundException, ValidationException{


		SubjectAndBody subjAndBody;
		Person creator = personService.get(task.getCreatedBy().getId());
		if (task.getType().equals(Task.CUSTOM_ACTION_PLAN_TASK)) {
			subjAndBody = messageTemplateService
					.createCustomActionPlanTaskMessage(new TaskMessageTemplateTO(task, creator));
		} else {
			subjAndBody = messageTemplateService
					.createActionPlanStepMessage(new TaskMessageTemplateTO(task, creator));
		}

		Message message = messageService.createMessage(task.getPerson(), null,
				subjAndBody);
		
		taskMessageSentService.save(new TaskMessageEnqueue(task, message, daysBefore));

	}
	
	

	@Override
	public Long getTaskCountForCoach(Person coach, Date createDateFrom, Date createDateTo, List<UUID> studentTypeIds) {
		return dao.getTaskCountForCoach(coach, createDateFrom, createDateTo, studentTypeIds);
	}
	 
	
	@Override
	public Long getStudentTaskCountForCoach(Person coach, Date createDateFrom, Date createDateTo, List<UUID> studentTypeIds) {
		return dao.getStudentTaskCountForCoach(coach, createDateFrom, createDateTo, studentTypeIds);
	}

	@Override
	public Pair<Long, Long> getOpenVsClosedTaskCountsForPerson(Person person) {
		return dao.getOpenVsClosedTaskCountsForPerson(person);
	}

	@Override
	public PagingWrapper<EntityStudentCountByCoachTO> getStudentTaskCountForCoaches(EntityCountByCoachSearchForm form) {
		return dao.getStudentTaskCountForCoaches(form);
	}
	
	public List<Task> create(List<TaskTO> taskTOs, Person student) throws ValidationException, 
	ObjectNotFoundException{
		List<Task> tasks = new ArrayList<Task>();
		
		for(TaskTO taskTO:taskTOs){
			if (taskTO.getId() != null) {
				throw new ValidationException(
						"It is invalid to send with an ID to the create method. Did you mean to use the save method instead?");
			}
			final Task model = factory.from(taskTO);
			if (null != model) {
				if (model.getPerson() == null) {
					model.setPerson(student);
				}

				final Task createdModel = dao.save(model);
				if (null != createdModel) {
					tasks.add(createdModel);
				}
			}
		}
		return tasks;
	}
	

}
