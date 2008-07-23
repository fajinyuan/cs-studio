package org.csstudio.nams.service.configurationaccess.localstore;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.csstudio.nams.service.configurationaccess.localstore.declaration.AlarmbearbeiterDTO;
import org.csstudio.nams.service.configurationaccess.localstore.declaration.AlarmbearbeiterGruppenDTO;
import org.csstudio.nams.service.configurationaccess.localstore.declaration.Configuration;
import org.csstudio.nams.service.configurationaccess.localstore.declaration.FilterDTO;
import org.csstudio.nams.service.configurationaccess.localstore.declaration.HistoryDTO;
import org.csstudio.nams.service.configurationaccess.localstore.declaration.LocalStoreConfigurationService;
import org.csstudio.nams.service.configurationaccess.localstore.declaration.NewAMSConfigurationElementDTO;
import org.csstudio.nams.service.configurationaccess.localstore.declaration.ReplicationStateDTO;
import org.csstudio.nams.service.configurationaccess.localstore.declaration.TopicDTO;
import org.csstudio.nams.service.configurationaccess.localstore.declaration.exceptions.InconsistentConfigurationException;
import org.csstudio.nams.service.configurationaccess.localstore.declaration.exceptions.StorageError;
import org.csstudio.nams.service.configurationaccess.localstore.declaration.exceptions.StorageException;
import org.csstudio.nams.service.configurationaccess.localstore.declaration.exceptions.UnknownConfigurationElementError;
import org.csstudio.nams.service.configurationaccess.localstore.internalDTOs.FilterConditionDTO;
import org.csstudio.nams.service.configurationaccess.localstore.internalDTOs.RubrikDTO;
import org.csstudio.nams.service.configurationaccess.localstore.internalDTOs.User2UserGroupDTO;
import org.csstudio.nams.service.configurationaccess.localstore.internalDTOs.filterConditionSpecifics.FilterConditionsToFilterDTO;
import org.csstudio.nams.service.configurationaccess.localstore.internalDTOs.filterConditionSpecifics.HasJoinedElements;
import org.csstudio.nams.service.configurationaccess.localstore.internalDTOs.filterConditionSpecifics.JunctorConditionDTO;
import org.csstudio.nams.service.configurationaccess.localstore.internalDTOs.filterConditionSpecifics.JunctorConditionForFilterTreeDTO;
import org.csstudio.nams.service.configurationaccess.localstore.internalDTOs.filterConditionSpecifics.NegationConditionForFilterTreeDTO;
import org.csstudio.nams.service.configurationaccess.localstore.internalDTOs.filterConditionSpecifics.StringArrayFilterConditionCompareValuesDTO;
import org.csstudio.nams.service.configurationaccess.localstore.internalDTOs.filterConditionSpecifics.StringArrayFilterConditionDTO;
import org.csstudio.nams.service.configurationaccess.localstore.internalDTOs.filterConditionSpecifics.StringFilterConditionDTO;
import org.csstudio.nams.service.logging.declaration.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Transaction;
import org.hibernate.classic.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

/**
 * Implementation für Hibernate.
 * 
 * TODO Rename to ConfigurationStoreServiceHibernateImpl
 */
class LocalStoreConfigurationServiceImpl implements
		LocalStoreConfigurationService {

	private final Session session;
	private final Logger logger;

	/**
	 * 
	 * @param session
	 *            The session to work on; the session will be treated as
	 *            exclusive instance and be closed on finalization of this
	 *            service instance.
	 * @param logger
	 */
	public LocalStoreConfigurationServiceImpl(final Session session,
			Logger logger) {
		this.session = session;
		this.logger = logger;
	}

	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		session.close();
	}

	public ReplicationStateDTO getCurrentReplicationState()
			throws StorageError, StorageException,
			InconsistentConfigurationException {
		ReplicationStateDTO result = null;
		try {
			final Transaction newTransaction = this.session.beginTransaction();
			final List<?> messages = this.session.createQuery(
					"from ReplicationStateDTO r where r.flagName = '"
							+ ReplicationStateDTO.DB_FLAG_NAME + "'").list();

			if (!messages.isEmpty()) {
				result = (ReplicationStateDTO) messages.get(0);
			}
			newTransaction.commit();

		} catch (Throwable t) {
			new StorageError("Failed to write replication flag", t);
		}

		if (result == null) {
			throw new InconsistentConfigurationException(
					"Replication state unavailable.");
		}

		return result;
	}

	// private void test() {
	// Transaction tx = session.beginTransaction();
	// TopicDTO message = new TopicDTO();
	// Integer msgId = (Integer) session.save(message);
	// System.out.println("New TOPIC id: " + msgId);
	// tx.commit();
	//
	// // Second unit of work
	//
	// Transaction newTransaction = session.beginTransaction();
	// List<?> messages = session.createQuery(
	// "from TopicDTO t order by t.id asc").list();
	// System.out.println(messages.size() + " TOPIC(s) found:");
	//
	// for (Iterator<?> iter = messages.iterator(); iter.hasNext();) {
	// TopicDTO loadedMsg = (TopicDTO) iter.next();
	// System.out.println(loadedMsg.toString());
	// }
	// newTransaction.commit();
	// }

	public Configuration getEntireConfiguration() throws StorageError,
			StorageException, InconsistentConfigurationException {
		final Transaction transaction = this.session.beginTransaction();
		Configuration result = null;
		try {
			// 
			Collection<AlarmbearbeiterDTO> alleAlarmbarbeiter;
			Collection<TopicDTO> alleAlarmtopics;
			Collection<AlarmbearbeiterGruppenDTO> alleAlarmbearbeiterGruppen;
			Collection<FilterDTO> allFilters;
			Collection<FilterConditionsToFilterDTO> allFilterConditionMappings;

			Collection<FilterConditionDTO> allFilterConditions;
			Collection<RubrikDTO> alleRubriken;
			List<User2UserGroupDTO> alleUser2UserGroupMappings;
			Collection<StringArrayFilterConditionCompareValuesDTO> allCompareValues;

			// PUBLICs
			alleAlarmbarbeiter = session.createCriteria(
					AlarmbearbeiterDTO.class).addOrder(Order.asc("userId"))
					.list();
			alleAlarmbearbeiterGruppen = session.createCriteria(
					AlarmbearbeiterGruppenDTO.class).addOrder(
					Order.asc("userGroupId")).list();

			alleAlarmtopics = session.createCriteria(TopicDTO.class).list();
			allFilters = session.createCriteria(FilterDTO.class).list();

			allFilterConditions = session.createCriteria(
					FilterConditionDTO.class).addOrder(
					Order.asc("iFilterConditionID")).list();
			
			// Nots befüllen
			for (FilterConditionDTO filterConditionDTO : allFilterConditions) {
				if( filterConditionDTO instanceof NegationConditionForFilterTreeDTO ) {
					NegationConditionForFilterTreeDTO notCond = (NegationConditionForFilterTreeDTO) filterConditionDTO;
					for (FilterConditionDTO possibleCondition : allFilterConditions) {
						if( notCond.getINegatedFCRef() == possibleCondition.getIFilterConditionID() ) {
							notCond.setNegatedFilterCondition(possibleCondition);
							break;
						}
					}
				}
			}
			
			alleRubriken = session.createCriteria(RubrikDTO.class).list();

			alleUser2UserGroupMappings = session.createCriteria(
					User2UserGroupDTO.class).list();

			// Collection<FilterConditionTypeDTO> allFilterConditionsTypes =
			// session
			// .createCriteria(FilterConditionTypeDTO.class).list();
			// pruefeUndOrdneTypenDenFilterConditionsZu(allFilterConditionsTypes);

			allFilterConditionMappings = session.createCriteria(
					FilterConditionsToFilterDTO.class).list();
			pruefeUndOrdnerFilterDieFilterConditionsZu(
					allFilterConditionMappings, allFilterConditions, allFilters);
			setChildFilterConditionsInJunctorDTOs(allFilterConditions);
			allCompareValues = session.createCriteria(
					StringArrayFilterConditionCompareValuesDTO.class).list();
			setStringArrayCompareValues(allCompareValues, allFilterConditions);

			Collection<FilterConditionDTO> allFCs = new HashSet<FilterConditionDTO>(
					allFilterConditions);
			for (FilterConditionDTO fc : allFCs) {
				if (fc instanceof JunctorConditionForFilterTreeDTO) {
					JunctorConditionForFilterTreeDTO jcfft = (JunctorConditionForFilterTreeDTO) fc;
					try {
						jcfft.loadJoinData(session, allFCs);
					} catch (Throwable e) {
						throw new InconsistentConfigurationException(
								"unable to load joined conditions of JunctionConditionForFilters",
								e);
					}
				}
			}
			addUsersToGroups(session, alleAlarmbearbeiterGruppen,
					alleUser2UserGroupMappings);

			//
			result = new Configuration(alleAlarmbarbeiter, alleAlarmtopics,
					alleAlarmbearbeiterGruppen, allFilters,
					allFilterConditionMappings, allFilterConditions,
					alleRubriken, alleUser2UserGroupMappings, allCompareValues);
			transaction.commit();
		} catch (Throwable t) {
			transaction.rollback();
			t.printStackTrace();
		}
		return result;
	}

	private void addUsersToGroups(Session session,
			Collection<AlarmbearbeiterGruppenDTO> alleAlarmbearbeiterGruppen,
			List<User2UserGroupDTO> alleUser2UserGroupMappings) {
		HashMap<Integer, AlarmbearbeiterGruppenDTO> gruppen = new HashMap<Integer, AlarmbearbeiterGruppenDTO>();
		for (AlarmbearbeiterGruppenDTO gruppe : alleAlarmbearbeiterGruppen) {
			gruppen.put(gruppe.getUserGroupId(), gruppe);
		}
		for (User2UserGroupDTO map : alleUser2UserGroupMappings) {
			try {
				gruppen.get(map.getUser2UserGroupPK().getIUserGroupRef())
						.alarmbearbeiterZuordnen(map);
			} catch (NullPointerException npe) {
				session.delete(map);
				logger.logWarningMessage(this,
						"Deleted invalid User To UserGroup mapping, group "
								+ map.getUser2UserGroupPK().getIUserGroupRef()
								+ " doesn't exist");
			}
		}
	}

	private void setStringArrayCompareValues(
			Collection<StringArrayFilterConditionCompareValuesDTO> allCompareValues,
			Collection<FilterConditionDTO> allFilterConditions) {
		Map<Integer, StringArrayFilterConditionDTO> stringAFC = new HashMap<Integer, StringArrayFilterConditionDTO>();
		for (FilterConditionDTO filterCondition : allFilterConditions) {
			if (filterCondition instanceof StringArrayFilterConditionDTO) {
				stringAFC.put(filterCondition.getIFilterConditionID(),
						(StringArrayFilterConditionDTO) filterCondition);
				StringArrayFilterConditionDTO sFC = (StringArrayFilterConditionDTO) filterCondition;
				sFC
						.setCompareValues(new LinkedList<StringArrayFilterConditionCompareValuesDTO>());
			}
		}
		for (StringArrayFilterConditionCompareValuesDTO stringArrayFilterConditionCompareValuesDTO : allCompareValues) {
			StringArrayFilterConditionDTO conditionDTO = stringAFC
					.get(stringArrayFilterConditionCompareValuesDTO
							.getFilterConditionRef());
			List<StringArrayFilterConditionCompareValuesDTO> list = conditionDTO
					.getCompareValueList();
			list.add(stringArrayFilterConditionCompareValuesDTO);
			conditionDTO.setCompareValues(list);
		}

	}

	private void setChildFilterConditionsInJunctorDTOs(
			Collection<FilterConditionDTO> allFilterConditions) {
		for (FilterConditionDTO filterCondition : allFilterConditions) {
			if (filterCondition instanceof JunctorConditionDTO) {
				JunctorConditionDTO junctorConditionDTO = (JunctorConditionDTO) filterCondition;
				FilterConditionDTO firstFilterCondition = getFilterConditionForId(
						junctorConditionDTO.getFirstFilterConditionRef(),
						allFilterConditions);
				FilterConditionDTO secondFilterCondition = getFilterConditionForId(
						junctorConditionDTO.getSecondFilterConditionRef(),
						allFilterConditions);

				junctorConditionDTO
						.setFirstFilterCondition(firstFilterCondition);
				junctorConditionDTO
						.setSecondFilterCondition(secondFilterCondition);
			}
		}
	}

	public FilterConditionDTO getFilterConditionForId(int id,
			Collection<FilterConditionDTO> allFilterConditions) {
		for (FilterConditionDTO filterCondition : allFilterConditions) {
			if (filterCondition.getIFilterConditionID() == id) {
				return filterCondition;
			}
		}
		return null;
	}

	private void pruefeUndOrdnerFilterDieFilterConditionsZu(
			Collection<FilterConditionsToFilterDTO> allFilterConditionToFilter,
			Collection<FilterConditionDTO> allFilterConditions,
			Collection<FilterDTO> allFilters) {
		Map<Integer, FilterDTO> filters = new HashMap<Integer, FilterDTO>();
		for (FilterDTO filter : allFilters) {
			List<FilterConditionDTO> list = filter.getFilterConditions();
			list.clear();
			filter.setFilterConditions(list);
			filters.put(filter.getIFilterID(), filter);
		}
		for (FilterConditionsToFilterDTO filterConditionsToFilterDTO : allFilterConditionToFilter) {
			FilterDTO filterDTO = filters.get(filterConditionsToFilterDTO
					.getIFilterRef());
			if (filterDTO == null) {
				logger.logWarningMessage(this, "no filter found for id: "
						+ filterConditionsToFilterDTO.getIFilterRef());
			} else {
				List<FilterConditionDTO> filterConditions = filterDTO
						.getFilterConditions();
				filterConditions.add(getFilterConditionForId(
						filterConditionsToFilterDTO.getIFilterConditionRef(),
						allFilterConditions));
				filterDTO.setFilterConditions(filterConditions);
			}
		}
	}

	@SuppressWarnings("unchecked")
	public List<JunctorConditionDTO> getJunctorConditionDTOConfigurations() {
		final Transaction newTransaction = this.session.beginTransaction();
		final List<JunctorConditionDTO> junctorConditionDTOs = this.session
				.createQuery("from JunctorConditionDTO").list();

		newTransaction.commit();
		System.out.println(junctorConditionDTOs.toString());

		return junctorConditionDTOs;
	}

	public void saveJunctorConditionDTO(JunctorConditionDTO junctorConditionDTO) {
		Transaction tx = session.beginTransaction();
		Integer msgId = (Integer) session.save(junctorConditionDTO);
		// final List<?> messages = this.session.createQuery(
		// "from JunctorConditionDTO r where r.iFilterConditionID = '"+
		// junctorConditionDTO.getIFilterConditionID()+"'").list();
		System.out.println("New JunctorConditionDTO id: " + msgId);
		tx.commit();
	}

	// public TopicDTO getTopicConfigurations(final TopicConfigurationId id) {
	// final Transaction newTransaction = this.session.beginTransaction();
	// final List<?> messages = this.session.createQuery(
	// "from TopicDTO t where t.id = " + id.asDatabaseId()).list();
	// System.out.println(messages.size() + " TOPIC(s) found:");
	//
	// TopicDTO result = null;
	// if (!messages.isEmpty()) {
	// result = (TopicDTO) messages.get(0);
	// }
	// newTransaction.commit();
	//
	// return result;
	// }

	public void saveCurrentReplicationState(
			final ReplicationStateDTO currentState) throws StorageError,
			StorageException, UnknownConfigurationElementError {

		final Transaction newTransaction = this.session.beginTransaction();
		this.session.saveOrUpdate(currentState);
		newTransaction.commit();
		// throw new RuntimeException("Not implemented yet.");
	}

	@SuppressWarnings("unchecked")
	public List<FilterConditionDTO> getFilterConditionDTOConfigurations() {
		final Transaction newTransaction = this.session.beginTransaction();
		final List<FilterConditionDTO> vrs = this.session.createQuery(
				"from FilterConditionDTO t").list();
		System.out.println(vrs.size() + " FilterConditionDTO(s) found:");

		newTransaction.commit();

		return vrs;
	}

	@SuppressWarnings("unchecked")
	public List<StringArrayFilterConditionDTO> getStringArrayFilterConditionDTOConfigurations() {
		final Transaction newTransaction = this.session.beginTransaction();
		final List<StringArrayFilterConditionDTO> vrs = this.session
				.createQuery("from StringArrayFilterConditionDTO t").list();
		System.out.println(vrs.size()
				+ " StringArrayFilterConditionDTO(s) found:");

		newTransaction.commit();

		return vrs;
	}

	// public void saveStringFilterConditionDTO(
	// StringFilterConditionDTO stringConditionDTO) {
	// Transaction tx = session.beginTransaction();
	// Integer msgId = (Integer) session.save(stringConditionDTO);
	// // final List<?> messages = this.session.createQuery(
	// // "from JunctorConditionDTO r where r.iFilterConditionID = '"+
	// // junctorConditionDTO.getIFilterConditionID()+"'").list();
	// System.out.println("New StringrConditionDTO id: " + msgId);
	// tx.commit();
	//
	// }

	@SuppressWarnings("unchecked")
	public List<StringFilterConditionDTO> getStringFilterConditionDTOConfigurations() {
		final Transaction newTransaction = this.session.beginTransaction();
		final List<StringFilterConditionDTO> stringFilterConditionDTOs = this.session
				.createQuery("from StringFilterConditionDTO").list();

		newTransaction.commit();
		System.out.println(stringFilterConditionDTOs.toString());

		return stringFilterConditionDTOs;
	}

	public void saveHistoryDTO(HistoryDTO historyDTO) {
		Transaction tx = session.beginTransaction();
		session.save(historyDTO);
		tx.commit();
	}

	public void saveDTO(NewAMSConfigurationElementDTO dto) throws StorageError,
			StorageException, InconsistentConfigurationException {
		Transaction transaction = null;
		try {
			transaction = session.beginTransaction();
			saveDTONoTransaction(dto);
			if (dto instanceof StringArrayFilterConditionDTO) {
				StringArrayFilterConditionDTO filterDto = (StringArrayFilterConditionDTO) dto;
				List<StringArrayFilterConditionCompareValuesDTO> list = session
						.createCriteria(
								StringArrayFilterConditionCompareValuesDTO.class)
						.list();
				for (StringArrayFilterConditionCompareValuesDTO stringArrayFilterConditionCompareValuesDTO : list) {
					if (filterDto.getIFilterConditionID() == stringArrayFilterConditionCompareValuesDTO
							.getFilterConditionRef()) {
						session
								.delete(stringArrayFilterConditionCompareValuesDTO);
					}
				}
				for (StringArrayFilterConditionCompareValuesDTO compValue : filterDto
						.getCompareValueList()) {
					compValue.setPK(filterDto.getIFilterConditionID());
					session.saveOrUpdate(compValue);
				}
			}
		} catch (Throwable t) {
			if (transaction != null) {
				transaction.rollback();
			}
			throw new StorageException(
					"failed to save configuration element of type "
							+ dto.getClass().getSimpleName(), t);
		}
	}

	/**
	 * Für interne Verwendung innerhalb einer Transaction, da Transaction in
	 * JDBC nicht verschachtelt werden dürfen.
	 * 
	 * @see {@link #saveDTO(NewAMSConfigurationElementDTO)}
	 */
	protected void saveDTONoTransaction(NewAMSConfigurationElementDTO dto)
			throws Throwable {
		session.saveOrUpdate(dto);

		if (dto instanceof HasJoinedElements) {
			((HasJoinedElements<?>) dto).storeJoinLinkData(session);
		}
	}

	void deleteDTONoTransaction(NewAMSConfigurationElementDTO dto)
			throws Throwable {
		if (dto instanceof HasJoinedElements) {
			((HasJoinedElements<?>) dto).deleteJoinLinkData(session);
		}

		session.delete(dto);
	}

	public void deleteDTO(NewAMSConfigurationElementDTO dto)
			throws StorageError, StorageException,
			InconsistentConfigurationException {
		Transaction transaction = null;
		try {
			transaction = session.beginTransaction();
			deleteDTONoTransaction(dto);

			transaction.commit();
		} catch (Throwable t) {
			if (transaction != null) {
				transaction.rollback();
			}
			throw new StorageException(
					"failed to delete configuration element of type "
							+ dto.getClass().getSimpleName(), t);
		}
	}

	public AlarmbearbeiterDTO saveAlarmbearbeiterDTO(
			AlarmbearbeiterDTO alarmbearbeiterDTO) {

		Transaction tx = session.beginTransaction();
		Serializable generatedID = session.save(alarmbearbeiterDTO);
		tx.commit();
		return (AlarmbearbeiterDTO) session.load(AlarmbearbeiterDTO.class,
				generatedID);
	}

	public AlarmbearbeiterGruppenDTO saveAlarmbearbeiterGruppenDTO(
			AlarmbearbeiterGruppenDTO dto)
			throws InconsistentConfigurationException {

		Transaction tx = session.beginTransaction();
		try {
			session.saveOrUpdate(dto);

			Collection<User2UserGroupDTO> user2GroupMappings = session
					.createCriteria(User2UserGroupDTO.class).list();

			for (User2UserGroupDTO a : user2GroupMappings) {
				if (a.getUser2UserGroupPK().getIUserGroupRef() == dto
						.getUserGroupId()) {
					session.delete(a);
				}
			}
			// for all used FC
			// get the mappingDTO, if no DTO exists, create one
			Set<User2UserGroupDTO> zugehoerigeAlarmbearbeiter = dto
					.gibZugehoerigeAlarmbearbeiter();

			// save the used Mappings
			for (User2UserGroupDTO map : zugehoerigeAlarmbearbeiter) {
				session.save(map);
			}
			tx.commit();
		} catch (Throwable e) {
			tx.rollback();
			throw new InconsistentConfigurationException(e.getMessage());
		}
		return dto;
	}

	public TopicDTO saveTopicDTO(TopicDTO topicDTO) {
		Transaction tx = session.beginTransaction();
		Serializable generatedID = session.save(topicDTO);
		tx.commit();
		return (TopicDTO) session.load(TopicDTO.class, generatedID);

	}

	/**
	 * TODO Remove return value
	 * 
	 * @throws StorageException
	 */
	@Deprecated
	public FilterConditionDTO saveFilterCondtionDTO(
			FilterConditionDTO filterConditionDTO) throws StorageException {
		Transaction tx = null;
		try {
			tx = session.beginTransaction();
			// session.delete(filterConditionDTO);
			session.saveOrUpdate(filterConditionDTO);
			tx.commit();
		} catch (Throwable t) {
			if (tx != null) {
				tx.rollback();
			}
			throw new StorageException("failed to store fc", t);
		}
		return filterConditionDTO;
	}

	public RubrikDTO saveRubrikDTO(RubrikDTO dto) {
		Transaction tx = session.beginTransaction();
		Serializable generatedID = session.save(dto);
		tx.commit();
		return (RubrikDTO) session.load(RubrikDTO.class, generatedID);
	}

	@SuppressWarnings("unchecked")
	public FilterDTO saveFilterDTO(FilterDTO dto)
			throws InconsistentConfigurationException {
		Transaction tx = null;
		try {
			tx = session.beginTransaction();

			session.saveOrUpdate(dto);

			List<FilterConditionDTO> filterConditions = dto
					.getFilterConditions();

			Set<FilterConditionsToFilterDTO> zuSpeicherndeJoins = new HashSet<FilterConditionsToFilterDTO>();

			// erzeugen der zu schreibenden join daten
			for (FilterConditionDTO filterConditionDTO : filterConditions) {
				FilterConditionsToFilterDTO joinData = new FilterConditionsToFilterDTO();
				joinData.setIFilterRef(dto.getIFilterID());
				joinData.setIFilterConditionRef(filterConditionDTO
						.getIFilterConditionID());

				zuSpeicherndeJoins.add(joinData);

			}

			// clean up join data
			Collection<FilterConditionsToFilterDTO> conditionMappings = session
					.createCriteria(FilterConditionsToFilterDTO.class).list();

			for (FilterConditionsToFilterDTO joinElement : conditionMappings) {
				if (joinElement.getIFilterRef() == dto.getIFilterID()) {
					int filterConditionRef = joinElement
							.getIFilterConditionRef();

					if (!zuSpeicherndeJoins.contains(joinElement)) {
						session.delete(joinElement); // nicht mehr verwendete
														// Knoten löschen
					}

					Collection<JunctorConditionForFilterTreeDTO> junctorConditions = session
							.createCriteria(
									JunctorConditionForFilterTreeDTO.class)
							.add(Restrictions.idEq(filterConditionRef)).list();
					if (junctorConditions != null
							&& junctorConditions.size() > 0) {
						for (JunctorConditionForFilterTreeDTO junctorConditionForFilterTreeDTO : junctorConditions) {
							if (!filterConditions
									.contains(junctorConditionForFilterTreeDTO)) {
								deleteDTONoTransaction(junctorConditionForFilterTreeDTO);
							}
						}
					}

					Collection<NegationConditionForFilterTreeDTO> notConditions = session
							.createCriteria(
									NegationConditionForFilterTreeDTO.class)
							.add(Restrictions.idEq(filterConditionRef)).list();
					if (notConditions != null
							&& notConditions.size() > 0) {
						for (NegationConditionForFilterTreeDTO notConditionForFilterTreeDTO : notConditions) {
							if (!filterConditions
									.contains(notConditionForFilterTreeDTO)) {
								deleteDTONoTransaction(notConditionForFilterTreeDTO);
							}
						}
					}
				}
			}

			// join speichern
			for (FilterConditionDTO filterConditionDTO : filterConditions) {
				if (filterConditionDTO instanceof JunctorConditionForFilterTreeDTO ||
						filterConditionDTO instanceof NegationConditionForFilterTreeDTO) {
					// Diese Condition speichern, da sie von Editor angelegt
					// wird.
					saveDTONoTransaction(filterConditionDTO);
				}

				FilterConditionsToFilterDTO joinData = new FilterConditionsToFilterDTO();
				joinData.setIFilterRef(dto.getIFilterID());
				joinData.setIFilterConditionRef(filterConditionDTO
						.getIFilterConditionID());

				if (!zuSpeicherndeJoins.contains(joinData)) {
					saveDTONoTransaction(joinData);
				}
			}

			tx.commit();
		} catch (Throwable e) {
			e.printStackTrace();
			if (tx != null)
				tx.rollback();
			throw new InconsistentConfigurationException(e.getMessage());
		}
		return dto;
	}

	public void deleteAlarmbearbeiterDTO(AlarmbearbeiterDTO dto)
			throws InconsistentConfigurationException {
		Transaction tx = session.beginTransaction();
		try {
			session.delete(dto);
		} catch (HibernateException e) {
			new InconsistentConfigurationException("Could not delete " + dto
					+ ". \n It is still in use.");
		}
		tx.commit();
	}

	public void deleteAlarmbearbeiterGruppenDTO(AlarmbearbeiterGruppenDTO dto)
			throws InconsistentConfigurationException {
		Transaction tx = session.beginTransaction();
		try {
			Collection<User2UserGroupDTO> user2GroupMappings = session
					.createCriteria(User2UserGroupDTO.class).list();

			for (User2UserGroupDTO a : user2GroupMappings) {
				if (a.getUser2UserGroupPK().getIUserGroupRef() == dto
						.getUserGroupId()) {
					session.delete(a);
				}
			}
			session.delete(dto);
		} catch (HibernateException e) {
			new InconsistentConfigurationException("Could not delete " + dto
					+ ". \n It is still in use.");
		}
		tx.commit();
	}

	public void deleteAlarmtopicDTO(TopicDTO dto)
			throws InconsistentConfigurationException {
		Transaction tx = session.beginTransaction();
		try {
			session.delete(dto);
		} catch (HibernateException e) {
			new InconsistentConfigurationException("Could not delete " + dto
					+ ". \n It is still in use.");
		}
		tx.commit();
	}

	// TODO Exception Handling
	public void deleteFilterConditionDTO(FilterConditionDTO dto)
			throws InconsistentConfigurationException {
		Transaction tx = session.beginTransaction();
		try {
			session.delete(dto);
			tx.commit();
		} catch (HibernateException e) {
			tx.rollback();
			new InconsistentConfigurationException("Could not delete " + dto
					+ ". \n It is still in use.", e);
		}
	}

	public void deleteFilterDTO(FilterDTO dto)
			throws InconsistentConfigurationException, StorageError,
			StorageException {
		deleteDTO(dto);
	}

	public void prepareSynchonization() throws StorageError, StorageException,
			InconsistentConfigurationException {
		// TODO Hier die Syn-Tabellen anlegen / Datgen kopieren / GGf. über ein
		// HSQL-Statement.
	}
}
