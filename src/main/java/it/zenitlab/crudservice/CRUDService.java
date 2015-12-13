/*
 * Copyright 2015 Zenit Srl <www.zenitlab.it>.
 *
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
 */

package it.zenitlab.crudservice;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import it.zenitlab.crudservice.exception.InvalidClassException;
import it.zenitlab.crudservice.exception.InvalidParameterException;
import it.zenitlab.crudservice.exception.ServiceException;
import it.zenitlab.util.criteria.FilterCondition;
import it.zenitlab.util.criteria.SortingVerse;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import org.apache.log4j.Logger;

/**
 * Abstract class implementing basic CRUD operations
 *
 * @author Giuseppe M. Mazzeo <gmmazzeo@gmail.com>
 * @author Michele Milidoni <michelemilidoni@gmail.com>
 */
public abstract class CRUDService implements AutoCloseable {

    final static public int CREATE = 1, UPDATE = 2, ASC = 1, DESC = -1;
    protected EntityManager em;
    protected Class entityClass;

    public CRUDService(Class entityClass) {
        em = EM.getInstance();
        this.entityClass = entityClass;
    }

    public CRUDService(EntityManager em, Class entityClass) {
        this.em = em;
        this.entityClass = entityClass;
    }

    /**
     * Se esiste una transazione attiva alla chiamata del metodo, essa sarà
     * usata per la creazione dell'oggetto, altrimenti viene creata una nuova
     * transazione. L'oggetto viene prima validato attraverso il metodo astratto
     * validate. Quindi, viene invocato un altro metodo astratto, beforePersist.
     * Successivamente, l'oggetto è resto persistente e viene invocato un altro
     * metodo astratto, afterPersist. Se viene lanciata un'eccezione durante le
     * diverse fasi, viene effettuato il roll back, a meno che non esistesse già
     * una transazione attiva al momento dell'invocazione del metodo, nel qual
     * caso l'eccezione viene lanciata del metodo. Se tutte le fasi si
     * completano correttamente, viene effettuato il commit della transazione, a
     * meno che non esistesse già una transazione attiva al momento
     * dell'invocazione del metodo.
     *
     * @param <T>
     * @param o l'oggetto da rendere persistente
     * @param params un insieme di parametri (opzionali) da usare durante il
     * salvataggio
     * @return l'oggetto reso persistente
     * @throws it.zenitlab.crudservice.exception.ServiceException
     */
    public <T> T create(T o, HashMap<String, Object> params) throws ServiceException {
        if (!o.getClass().equals(entityClass)) {
            throw new InvalidClassException(entityClass, o.getClass(), "Classe non valida. Ricevuto oggetto " + o.getClass() + " invece di " + entityClass);
        }
        validate(o, CREATE, params);
        boolean activeTransaction = !beginTransaction();
        try {
            beforePersist(o, params);
            em.persist(o);
            afterPersist(o, params);
        } catch (ServiceException ipe) {
            if (!activeTransaction) {
                em.getTransaction().rollback();
            }
            throw ipe;
        } catch (Exception e) {
            if (!activeTransaction) {
                em.getTransaction().rollback();
            }
            Logger.getLogger(CRUDService.class).error("Unexpected error in CREATE", e);
            throw new ServiceException(e.getMessage(), "Unexpected error");
        }
        commitTransaction(activeTransaction);
        return o;
    }

    /**
     * Versione del metodo precedente senza parametri aggiuntivi
     *
     * @param <T>
     * @param o l'oggetto da rendere persistente
     * @return l'oggetto reso persistente
     * @throws it.zenitlab.crudservice.exception.ServiceException
     */
    public <T> T create(T o) throws ServiceException {
        return create(o, new HashMap<String, Object>());
    }

    /**
     * Legge l'oggetto con id uguale a quello passato
     *
     * @param <T>
     * @param id L'id dell'oggetto da leggere
     * @param params un insieme di parametri (opzionali) da usare durante la
     * lettura
     * @return L'oggetto letto, se esiste, oppure null
     */
    public <T> T read(int id, HashMap<String, Object> params) throws ServiceException {
        return (T) em.find(entityClass, id);
    }

    /**
     * Stessa versione del metodo precedente, ma senza parametri aggiuntivi.
     *
     * @param <T>
     * @param id
     * @return
     */
    public <T> T read(int id) throws ServiceException {
        return read(id, new HashMap<String, Object>());
    }

    /**
     * Elenco di tutti gli oggetti del tipo di riferimento.
     *
     * @param filter
     * @param order
     * @param start
     * @param limit
     * @return
     * @throws it.zenitlab.crudservice.exception.ServiceException
     */
    public List list(Collection<FilterCondition> filter, List<SortingVerse> order, Integer start, Integer limit) throws ServiceException {
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<Object> criteriaQuery = criteriaBuilder.createQuery();
        Root from = criteriaQuery.from(entityClass);
        criteriaQuery.select(from);
        if (filter != null && !filter.isEmpty()) {
            HashMap<String, Path> filterPaths = new HashMap<String, Path>();
            for (FilterCondition f : filter) {
                String attribute = f.getAttribute();
                String[] ss = attribute.split("\\.");
                if (ss.length == 1) {
                    filterPaths.put(attribute, from.get(attribute));
                } else {
                    Join j = from.join(ss[0], JoinType.LEFT);
                    for (int i = 1; i < ss.length - 1; i++) {
                        j = j.join(ss[i], JoinType.LEFT);
                    }
                    filterPaths.put(attribute, j.get(ss[ss.length - 1]));
                }
            }
            Predicate p = criteriaBuilder.conjunction();
            Gson gson = new GsonBuilder().setDateFormat("dd/MM/yyyy HH:mm").create();
            for (FilterCondition f : filter) {
                if (f.getOperandClassName() != null && !f.getOperandClassName().equals(f.getOperand().getClass().getName())) {
                    Class c1 = null;
                    try {
                        c1 = Class.forName(f.getOperandClassName());
                    } catch (ClassNotFoundException ex) {
                        Logger.getLogger(CRUDService.class).error("Class c1 not found in list", ex);
                    }
                    f.setOperand(gson.fromJson("\"" + f.getOperand().toString() + "\"", c1));
                }
                if (f.getOperand2() != null && f.getOperand2ClassName() != null && !f.getOperand2ClassName().equals(f.getOperand2().getClass().getName())) {
                    Class c2 = null;
                    try {
                        c2 = Class.forName(f.getOperand2ClassName());
                    } catch (ClassNotFoundException ex) {
                        Logger.getLogger(CRUDService.class).error("Class c2 not found in list", ex);
                    }
                    f.setOperand2(gson.fromJson("\"" + f.getOperand2().toString() + "\"", c2));
                }
                switch (f.getOperator()) {
                    case FilterCondition.EQ:
                        if (f.getIsCaseSensitive()) {
                            p = criteriaBuilder.and(p, criteriaBuilder.equal(filterPaths.get(f.getAttribute()), f.getOperand()));
                        } else {
                            Expression<String> literal = criteriaBuilder.upper(criteriaBuilder.literal((String) f.getOperand()));
                            p = criteriaBuilder.and(p, criteriaBuilder.equal(criteriaBuilder.upper(filterPaths.get(f.getAttribute())), literal));
                        }
                        break;
                    case FilterCondition.GE:
                        if (f.getIsCaseSensitive()) {
                            p = criteriaBuilder.and(p, criteriaBuilder.greaterThanOrEqualTo(filterPaths.get(f.getAttribute()), (Comparable) f.getOperand()));
                        } else {
                            Expression<String> literal = criteriaBuilder.upper(criteriaBuilder.literal((String) f.getOperand()));
                            p = criteriaBuilder.and(p, criteriaBuilder.greaterThanOrEqualTo(criteriaBuilder.upper(filterPaths.get(f.getAttribute())), literal));
                        }
                        break;
                    case FilterCondition.GT:
                        if (f.getIsCaseSensitive()) {
                            p = criteriaBuilder.and(p, criteriaBuilder.greaterThan(filterPaths.get(f.getAttribute()), (Comparable) f.getOperand()));
                        } else {
                            Expression<String> literal = criteriaBuilder.upper(criteriaBuilder.literal((String) f.getOperand()));
                            p = criteriaBuilder.and(p, criteriaBuilder.greaterThan(criteriaBuilder.upper(filterPaths.get(f.getAttribute())), literal));
                        }
                        break;
                    case FilterCondition.LT:
                        if (f.getIsCaseSensitive()) {
                            p = criteriaBuilder.and(p, criteriaBuilder.lessThan(filterPaths.get(f.getAttribute()), (Comparable) f.getOperand()));
                        } else {
                            Expression<String> literal = criteriaBuilder.upper(criteriaBuilder.literal((String) f.getOperand()));
                            p = criteriaBuilder.and(p, criteriaBuilder.lessThan(criteriaBuilder.upper(filterPaths.get(f.getAttribute())), literal));
                        }
                        break;
                    case FilterCondition.LE:
                        if (f.getIsCaseSensitive()) {
                            p = criteriaBuilder.and(p, criteriaBuilder.lessThanOrEqualTo(filterPaths.get(f.getAttribute()), (Comparable) f.getOperand()));
                        } else {
                            Expression<String> literal = criteriaBuilder.upper(criteriaBuilder.literal((String) f.getOperand()));
                            p = criteriaBuilder.and(p, criteriaBuilder.lessThanOrEqualTo(criteriaBuilder.upper(filterPaths.get(f.getAttribute())), literal));
                        }
                        break;
                    case FilterCondition.NEQ:
                        if (f.getIsCaseSensitive()) {
                            p = criteriaBuilder.and(p, criteriaBuilder.notEqual(filterPaths.get(f.getAttribute()), f.getOperand()));
                        } else {
                            Expression<String> literal = criteriaBuilder.upper(criteriaBuilder.literal((String) f.getOperand()));
                            p = criteriaBuilder.and(p, criteriaBuilder.notEqual(criteriaBuilder.upper(filterPaths.get(f.getAttribute())), literal));
                        }
                        break;
                    case FilterCondition.LK:
                        if (f.getIsCaseSensitive()) {
                            p = criteriaBuilder.and(p, criteriaBuilder.like(filterPaths.get(f.getAttribute()), (String) f.getOperand()));
                        } else {
                            Expression<String> literal = criteriaBuilder.upper(criteriaBuilder.literal((String) f.getOperand()));
                            p = criteriaBuilder.and(p, criteriaBuilder.like(criteriaBuilder.upper(filterPaths.get(f.getAttribute())), literal));
                        }
                        break;
                    case FilterCondition.NL:
                        p = criteriaBuilder.and(p, criteriaBuilder.isNull(filterPaths.get(f.getAttribute())));
                        break;
                    case FilterCondition.NNL:
                        p = criteriaBuilder.and(p, criteriaBuilder.isNotNull(filterPaths.get(f.getAttribute())));
                        break;
                    case FilterCondition.BT:
                        if (f.getIsCaseSensitive()) {
                            p = criteriaBuilder.and(p, criteriaBuilder.between(filterPaths.get(f.getAttribute()), (Comparable) f.getOperand(), (Comparable) f.getOperand2()));
                        } else {
                            Expression<String> literal = criteriaBuilder.upper(criteriaBuilder.literal((String) f.getOperand()));
                            Expression<String> literal2 = criteriaBuilder.upper(criteriaBuilder.literal((String) f.getOperand2()));
                            p = criteriaBuilder.and(p, criteriaBuilder.between(criteriaBuilder.upper(filterPaths.get(f.getAttribute())), literal, literal2));
                        }
                        break;
                    case FilterCondition.NEMPTY:
                        p = criteriaBuilder.and(p, criteriaBuilder.isNotEmpty(filterPaths.get(f.getAttribute())));
                        break;
                    case FilterCondition.EMPTY:
                        p = criteriaBuilder.and(p, criteriaBuilder.isEmpty(filterPaths.get(f.getAttribute())));
                        break;
                }
            }
            criteriaQuery.where(p);
        }
        if (order != null && !order.isEmpty()) {
            HashMap<String, Path> orderPaths = new HashMap<String, Path>();
            for (SortingVerse s : order) {
                String attribute = s.getAttribute();
                String[] ss = attribute.split("\\.");
                if (ss.length == 1) {
                    orderPaths.put(attribute, from.get(attribute));
                } else {
                    Join j = from.join(ss[0]);
                    for (int i = 1; i < ss.length - 1; i++) {
                        j = j.join(ss[i]);
                    }
                    orderPaths.put(attribute, j.get(ss[ss.length - 1]));
                }
            }
            ArrayList<Order> ord = new ArrayList<Order>();
            for (SortingVerse s : order) {
                if (s.getVersus() == SortingVerse.ASC) {
                    if (s.getIsCaseSensitive()) {
                        ord.add(criteriaBuilder.asc(orderPaths.get(s.getAttribute())));
                    } else {
                        ord.add(criteriaBuilder.asc(criteriaBuilder.upper(orderPaths.get(s.getAttribute()))));
                    }
                } else {
                    if (s.getIsCaseSensitive()) {
                        ord.add(criteriaBuilder.desc(orderPaths.get(s.getAttribute())));
                    } else {
                        ord.add(criteriaBuilder.desc(criteriaBuilder.upper(orderPaths.get(s.getAttribute()))));
                    }
                }
            }
            criteriaQuery.orderBy(ord);
        }
        Query q = em.createQuery(criteriaQuery);
        if (limit != null) {
            q.setMaxResults(limit);
        }
        if (start != null) {
            q.setFirstResult(start);
        }
        try {
            List res = q.getResultList();
            return res;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServiceException(e.getMessage(), "Unexpected error");
        }
    }

    /**
     * Conteggio di tutti gli oggetti del tipo di base.
     *
     * @param filter
     * @return
     */
    public long count(Collection<FilterCondition> filter) throws ServiceException {
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<Long> criteriaQuery = criteriaBuilder.createQuery(Long.class);
        Root from = criteriaQuery.from(entityClass);
        Expression countExpression = criteriaBuilder.count(from);
        criteriaQuery.select(countExpression);
        Gson gson = new GsonBuilder().setDateFormat("dd/MM/yyyy HH:mm").create();
        if (filter != null && !filter.isEmpty()) {
            HashMap<String, Path> filterPaths = new HashMap<String, Path>();
            for (FilterCondition f : filter) {
                String attribute = f.getAttribute();
                String[] ss = attribute.split("\\.");
                if (ss.length == 1) {
                    filterPaths.put(attribute, from.get(attribute));
                } else {
                    Join j = from.join(ss[0]);
                    for (int i = 1; i < ss.length - 1; i++) {
                        j = j.join(ss[i]);
                    }
                    filterPaths.put(attribute, j.get(ss[ss.length - 1]));
                }
            }
            Predicate p = criteriaBuilder.conjunction();
            for (FilterCondition f : filter) {
                if (f.getOperandClassName() != null && !f.getOperandClassName().equals(f.getOperand().getClass().getName())) {
                    Class c1 = null;
                    try {
                        c1 = Class.forName(f.getOperandClassName());
                    } catch (ClassNotFoundException ex) {
                        Logger.getLogger(CRUDService.class).error("Class c1 not found in count", ex);
                    }
                    f.setOperand(gson.fromJson("\"" + f.getOperand().toString() + "\"", c1));
                }
                if (f.getOperand2() != null && f.getOperandClassName() != null && !f.getOperandClassName().equals(f.getOperand().getClass().getName())) {
                    Class c2 = null;
                    try {
                        c2 = Class.forName(f.getOperandClassName());
                    } catch (ClassNotFoundException ex) {
                        Logger.getLogger(CRUDService.class).error("Class c2 not found in count", ex);
                    }
                    f.setOperand2(gson.fromJson("\"" + f.getOperand2().toString() + "\"", c2));
                }
                switch (f.getOperator()) {
                    case FilterCondition.EQ:
                        if (f.getIsCaseSensitive()) {
                            p = criteriaBuilder.and(p, criteriaBuilder.equal(filterPaths.get(f.getAttribute()), f.getOperand()));
                        } else {
                            Expression<String> literal = criteriaBuilder.upper(criteriaBuilder.literal((String) f.getOperand()));
                            p = criteriaBuilder.and(p, criteriaBuilder.equal(criteriaBuilder.upper(filterPaths.get(f.getAttribute())), literal));
                        }
                        break;
                    case FilterCondition.GE:
                        if (f.getIsCaseSensitive()) {
                            p = criteriaBuilder.and(p, criteriaBuilder.greaterThanOrEqualTo(filterPaths.get(f.getAttribute()), (Comparable) f.getOperand()));
                        } else {
                            Expression<String> literal = criteriaBuilder.upper(criteriaBuilder.literal((String) f.getOperand()));
                            p = criteriaBuilder.and(p, criteriaBuilder.greaterThanOrEqualTo(criteriaBuilder.upper(filterPaths.get(f.getAttribute())), literal));
                        }
                        break;
                    case FilterCondition.GT:
                        if (f.getIsCaseSensitive()) {
                            p = criteriaBuilder.and(p, criteriaBuilder.greaterThan(filterPaths.get(f.getAttribute()), (Comparable) f.getOperand()));
                        } else {
                            Expression<String> literal = criteriaBuilder.upper(criteriaBuilder.literal((String) f.getOperand()));
                            p = criteriaBuilder.and(p, criteriaBuilder.greaterThan(criteriaBuilder.upper(filterPaths.get(f.getAttribute())), literal));
                        }
                        break;
                    case FilterCondition.LT:
                        if (f.getIsCaseSensitive()) {
                            p = criteriaBuilder.and(p, criteriaBuilder.lessThan(filterPaths.get(f.getAttribute()), (Comparable) f.getOperand()));
                        } else {
                            Expression<String> literal = criteriaBuilder.upper(criteriaBuilder.literal((String) f.getOperand()));
                            p = criteriaBuilder.and(p, criteriaBuilder.lessThan(criteriaBuilder.upper(filterPaths.get(f.getAttribute())), literal));
                        }
                        break;
                    case FilterCondition.LE:
                        if (f.getIsCaseSensitive()) {
                            p = criteriaBuilder.and(p, criteriaBuilder.lessThanOrEqualTo(filterPaths.get(f.getAttribute()), (Comparable) f.getOperand()));
                        } else {
                            Expression<String> literal = criteriaBuilder.upper(criteriaBuilder.literal((String) f.getOperand()));
                            p = criteriaBuilder.and(p, criteriaBuilder.lessThanOrEqualTo(criteriaBuilder.upper(filterPaths.get(f.getAttribute())), literal));
                        }
                        break;
                    case FilterCondition.NEQ:
                        if (f.getIsCaseSensitive()) {
                            p = criteriaBuilder.and(p, criteriaBuilder.notEqual(filterPaths.get(f.getAttribute()), f.getOperand()));
                        } else {
                            Expression<String> literal = criteriaBuilder.upper(criteriaBuilder.literal((String) f.getOperand()));
                            p = criteriaBuilder.and(p, criteriaBuilder.notEqual(criteriaBuilder.upper(filterPaths.get(f.getAttribute())), literal));
                        }
                        break;
                    case FilterCondition.LK:
                        if (f.getIsCaseSensitive()) {
                            p = criteriaBuilder.and(p, criteriaBuilder.like(filterPaths.get(f.getAttribute()), (String) f.getOperand()));
                        } else {
                            Expression<String> literal = criteriaBuilder.upper(criteriaBuilder.literal((String) f.getOperand()));
                            p = criteriaBuilder.and(p, criteriaBuilder.like(criteriaBuilder.upper(filterPaths.get(f.getAttribute())), literal));
                        }
                        break;
                    case FilterCondition.BT:
                        if (f.getIsCaseSensitive()) {
                            p = criteriaBuilder.and(p, criteriaBuilder.between(filterPaths.get(f.getAttribute()), (Comparable) f.getOperand(), (Comparable) f.getOperand2()));
                        } else {
                            Expression<String> literal = criteriaBuilder.upper(criteriaBuilder.literal((String) f.getOperand()));
                            Expression<String> literal2 = criteriaBuilder.upper(criteriaBuilder.literal((String) f.getOperand2()));
                            p = criteriaBuilder.and(p, criteriaBuilder.between(criteriaBuilder.upper(filterPaths.get(f.getAttribute())), literal, literal2));
                        }
                        break;
                    case FilterCondition.NEMPTY:
                        p = criteriaBuilder.and(p, criteriaBuilder.isNotEmpty(filterPaths.get(f.getAttribute())));
                        break;
                    case FilterCondition.EMPTY:
                        p = criteriaBuilder.and(p, criteriaBuilder.isEmpty(filterPaths.get(f.getAttribute())));
                        break;

                }
            }
            criteriaQuery.where(p);
        }        
        Query q = em.createQuery(criteriaQuery);
        try {
        long res = (Long) q.getSingleResult();
        return res;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServiceException(e.getMessage(), "Unexpected error");
        }
    }

    public long lastPage(int resultsPerPage) throws ServiceException {
        long tot = count(null);
        return (long) Math.ceil(1.0 * tot / resultsPerPage);
    }

    public List listPage(long pageNumber, int resultsPerPage) {
        ArrayList<Object> res = new ArrayList<Object>();
        long offset = resultsPerPage * (pageNumber - 1);
        String query = "SELECT o FROM " + entityClass.getSimpleName() + " o OFFSET " + offset + " LIMIT " + pageNumber;
        Query q = em.createQuery(query);
        return q.getResultList();
    }

    public <T> T update(T o, HashMap<String, Object> params) throws ServiceException {
        if (!o.getClass().equals(entityClass)) {
            throw new InvalidClassException(entityClass, o.getClass(), "Classe non valida. Ricevuto oggetto " + o.getClass() + " invece di " + entityClass);
        }
        validate(o, UPDATE, params);
        boolean activeTransaction = em.getTransaction().isActive();
        if (!activeTransaction) {
            em.getTransaction().begin();
        }
        T p = null;
        try {
            beforeMerge(o, params);
            Class c = o.getClass();
            Method m = c.getMethod("getId");
            Integer id = (Integer) m.invoke(o);
            if (id == null) {
                throw new InvalidParameterException("id", null, "ID NULL");
            }
            p = (T) em.find(c, id);
            if (p == null) {
                throw new InvalidParameterException("id", id, "ID NON VALIDO");
            }
            bind(p, o, params);
            afterMerge(p, params);
        } catch (ServiceException e1) {
            if (!activeTransaction) {
                em.getTransaction().rollback();
            }
            throw e1;
        } catch (Exception e2) {
            if (!activeTransaction) {
                em.getTransaction().rollback();
            }
            Logger.getLogger(CRUDService.class).error("Unexpected error in UPDATE", e2);
            throw new ServiceException(e2.getMessage(), "Unexpected error");
        }
        if (!activeTransaction) {
            em.getTransaction().commit();
        }
        return p;
    }

    /**
     * Se esiste una transazione attiva alla chiamata del metodo, essa sarà
     * usata per la modifica dell'oggetto, altrimenti viene creata una nuova
     * transazione. L'oggetto viene prima validato attraverso il metodo astratto
     * validate. Quindi, viene invocato un altro metodo astratto, beforeMerge.
     * Successivamente, viene effettuato il merge dell'oggetto un altro metodo
     * astratto, afterMerge. Se viene lanciata un'eccezione durante le diverse
     * fasi, viene effettuato il roll back, a meno che non esistesse già una
     * transazione attiva al momento dell'invocazione del metodo, nel qual caso
     * l'eccezione viene lanciata del metodo. Se tutte le fasi si completano
     * correttamente, viene effettuato il commit della transazione, a meno che
     * non esistesse già una transazione attiva al momento dell'invocazione del
     * metodo.
     *
     * @param o l'oggetto da aggiornare
     * @return l'oggetto aggiornato
     * @throws it.zenitlab.crudservice.exception.ServiceException
     */
    public <T> T update(Object o) throws ServiceException {
        return (T)update(o, new HashMap<String, Object>());
    }

    /**
     * Se esiste una transazione attiva alla chiamata del metodo, essa sarà
     * usata per l'eliminazione dell'oggetto, altrimenti viene creata una nuova
     * transazione. Viene prima effettuato un controllo per verificare se
     * l'oggetto sia eliminabile attraverso il metodo astratto checkRemovable.
     * Quindi, viene invocato un altro metodo astratto, beforeRemove.
     * Successivamente, viene effettuata la rimozione dell'oggetto e infine vine
     * invocato un altro metodo astratto, afterRemove. Se viene lanciata
     * un'eccezione durante le diverse fasi, viene effettuato il roll back, a
     * meno che non esistesse già una transazione attiva al momento
     * dell'invocazione del metodo, nel qual caso l'eccezione viene lanciata del
     * metodo. Se tutte le fasi si completano correttamente, viene effettuato il
     * commit della transazione, a meno che non esistesse già una transazione
     * attiva al momento dell'invocazione del metodo.
     *
     * @param id
     * @param params un insieme di parametri (opzionali) da usare durante la
     * rimozione
     * @return l'oggetto rimosso
     * @throws it.zenitlab.crudservice.exception.ServiceException
     */
    public Object delete(int id, HashMap<String, Object> params) throws ServiceException {
        boolean activeTransaction = em.getTransaction().isActive();
        if (!activeTransaction) {
            em.getTransaction().begin();
        }
        Object o;
        try {
            o = em.find(entityClass, id);
            checkRemovable(o, params);
            beforeRemove(o, params);
            em.remove(o);
            afterRemove(o, params);
        } catch (ServiceException ex) {
            if (!activeTransaction) {
                em.getTransaction().rollback();
            }
            throw ex;
        } catch (Exception ex) {
            if (!activeTransaction) {
                em.getTransaction().rollback();
            }
            Logger.getLogger(CRUDService.class).error("Unexpected error in DELETE", ex);
            throw new ServiceException(ex.getMessage(), "Unexpected error");
        }
        if (!activeTransaction) {
            em.getTransaction().commit();
        }
        return o;
    }

    public Object delete(int id) throws ServiceException {
        return delete(id, new HashMap<String, Object>());
    }

    /**
     * Questo metodo deve essere usato per verificare la validità formale
     * dell'oggetto da salvare. Ad esempio, per verificare se una stringa sia
     * non nulla, o di lunghezza valida. Generalmente, non deve essere usato per
     * controllare l'esistenza di oggetti correlati. Ad esempio, nel salvataggio
     * di una fattura, qualora il cliente sia obbligatorio, attraverso questo
     * metodo si controlla se l'id del cliente è impostato, ma non se a quell'id
     * corrisponda effettivamente un cliente. Tale controllo, deve essere
     * effettuato (generalmente) dal metodo beforePersist o beforeMerge.
     *
     * @param o the object to create orception
     * @param operationType
     * @param params
     * @throws it.zenitlab.crudservice.exception.ServiceException
     */
    public abstract void validate(Object o, int operationType, HashMap<String, Object> params) throws ServiceException;

    public void validate(Object o, int operationType) throws ServiceException {
        validate(o, operationType, new HashMap<String, Object>());
    }

    /**
     * Questo metodo si occupa di controllare e impostare correttamente gli
     * oggetti correlati e di valorizzare eventuali campi derivati (es., un
     * prezzo totale come trodotto tra quantità e prezzo unitario). Nel caso di
     * campi correlati (es., il cliente di una fattura), con questo metodo verrà
     * ricercato l'oggetto correlato (es., il cliente) e nel caso sia trovato,
     * viene assegnato all'oggetto da salvare (es., la fattura), altrimenti può
     * essere lanciata un'eccezione di oggetto correlato inesistente. La
     * necessità di assegnare l'oggetto correlato recuperato dal DB al'oggetto
     * da salvare è dovuta al fatto che l'oggetto correlato contenuto
     * nell'oggetto passato come parametro potrebbe non essere un oggetto
     * persistente.
     *
     * @param o
     * @param params
     * @throws it.zenitlab.crudservice.exception.ServiceException
     */
    public abstract void beforePersist(Object o, HashMap<String, Object> params) throws ServiceException;

    /**
     * Questo metodo si occupa di aggiornare i collegamenti tra oggetti. In
     * particolare, salvando un nuovo oggetto, attraverso questo metodo si
    Service * aggiornano gli insiemi degli oggetti correlati che hanno una relazione
     * uno a molti con l'oggetto salvato (es., insieme di fatture di un cliente
     * al salvataggio di una fattura). Inoltre, è possibile salvare gli oggetti
     * dipendenti dall'oggetto salvato (es., voci fattura quando si salva una
     * fattura).
     *
     * @param o
     * @param params
     * @throws it.zenitlab.crudservice.exception.ServiceException
     */
    public abstract void afterPersist(Object o, HashMap<String, Object> params) throws ServiceException;

    /**
     * This method is called at the beginning of update()
     *
     * @param o
     * @param params
     * @throws it.zenitlab.crudservice.exception.ServiceException
     */
    public abstract void beforeMerge(Object o, HashMap<String, Object> params) throws ServiceException;

    /**
     * This method is called at the end of update()
     *
     * @param o
     * @param params
     * @throws it.zenitlab.crudservice.exception.ServiceException
     */
    public abstract void afterMerge(Object o, HashMap<String, Object> params) throws ServiceException;

    /**
     * This method is called at the beginning of delete()
     *
     * @param o
     * @param params
     * @throws it.zenitlab.crudservice.exception.ServiceException
     */
    public abstract void beforeRemove(Object o, HashMap<String, Object> params) throws ServiceException;

    /**
     * This method is called at the end of delete()
     *
     * @param o
     * @param params
     * @throws it.zenitlab.crudservice.exception.ServiceException
     */
    public abstract void afterRemove(Object o, HashMap<String, Object> params) throws ServiceException;

    /**
     * This method is invoked to check whether an object can be removed
     *
     * @param o the object to delete
     * @param params
     * @throws it.zenitlab.crudservice.exception.ServiceException
     * @throws Exception
     */
    public abstract void checkRemovable(Object o, HashMap<String, Object> params) throws ServiceException;

    /**
     * This method copy the attributes of source over target
     *
     * @param target
     * @param source
     * @param params
     * @throws it.zenitlab.crudservice.exception.ServiceException
     */
    public abstract void bind(Object target, Object source, HashMap<String, Object> params) throws ServiceException;

    public HashSet newObjects(Set s) throws NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        HashSet res = new HashSet();
        if (s == null || s.isEmpty()) {
            return res;
        }
        Class c = s.iterator().next().getClass();
        Method m = c.getMethod("getId");
        for (Object o : s) {
            if (m.invoke(o) == null) {
                res.add(o);
            }
        }
        return res;
    }

    public HashSet oldObjects(Set s) throws NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        HashSet res = new HashSet();
        if (s == null || s.isEmpty()) {
            return res;
        }
        Class c = s.iterator().next().getClass();
        Method m = c.getMethod("getId");
        for (Object o : s) {
            if (m.invoke(o) != null) {
                res.add(o);
            }
        }
        return res;
    }

    public HashSet deletedObjects(Set oldObjects, Set newObjects) {
        HashSet res = new HashSet();
        if (oldObjects == null || oldObjects.isEmpty()) {
            return res;
        }
        if (newObjects == null) {
            newObjects = new HashSet();
        }
        for (Object o : oldObjects) {
            if (!newObjects.contains(o)) {
                res.add(o);
            }
        }
        return res;
    }

    public <T> T getManagedEntity(T o) throws ServiceException {
        if (em.contains(o)) {
            return o;
        }
        Class c = o.getClass();
        while (c.getName().contains("$")) {
            c = c.getSuperclass();
        }
        try {
            Method m = c.getMethod("getId");
            Integer id = (Integer) m.invoke(o);
            if (id == null) {
                throw new InvalidParameterException("id", null, "ID NULL");
            }
            o = (T) em.find(c, id);
            if (o == null) {
                throw new InvalidParameterException("id", id, "ID NON VALIDO "+c.getName()+":"+id);
            }
            return o;
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException("Si è verificato un errore di reflection: " + e.getMessage(), "Errore interno");
        }
    }

    @Override    
    public void close() {
        if (em.isOpen()) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
        }
    }

    public boolean beginTransaction() {
        if (em.getTransaction().isActive()) {
            return false;
        }
        em.getTransaction().begin();
        return true;
    }

    public void commitTransaction() {
        commitTransaction(false);
    }

    public void commitTransaction(boolean skipCommit) {
        if (skipCommit) {
            return;
        }
        if (em.getTransaction().isActive()) {
            em.getTransaction().commit();
        }
    }

    public void rollbackTransaction() {
        rollbackTransaction(false);
    }

    public void rollbackTransaction(boolean skipRollback) {
        if (skipRollback) {
            return;
        }
        if (em.getTransaction().isActive()) {
            em.getTransaction().rollback();
        }
    }

    public void initNullCollections(Object o) throws ServiceException {
        if (o == null) {
            return;
        }
        for (Field f : o.getClass().getDeclaredFields()) {
            if (Set.class.isAssignableFrom(f.getType())) {
                boolean accessibile = f.isAccessible();
                if (!accessibile) {
                    f.setAccessible(true);
                }
                try {
                    if (f.get(o) == null) {
                        f.set(o, new HashSet());
                    }
                } catch (Exception e) {
                    System.out.println(e);
                }
                if (!accessibile) {
                    f.setAccessible(false);
                }
                //System.out.println(f.getName());
            } else if (List.class.isAssignableFrom(f.getType())) {
                boolean accessibile = f.isAccessible();
                if (!accessibile) {
                    f.setAccessible(true);
                }
                try {
                    if (f.get(o) == null) {
                        f.set(o, new ArrayList());
                    }
                } catch (Exception e) {
                    System.out.println(e);
                }
                if (!accessibile) {
                    f.setAccessible(false);
                }
                System.out.println(f.getName());
            }
        }
    }    
}