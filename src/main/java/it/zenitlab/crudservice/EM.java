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

import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

/**
 *
 * @author Giuseppe M. Mazzeo <gmmazzeo@gmail.com>
 * @author Michele Milidoni <michelemilidoni@gmail.com>
 */
public class EM {

    private static EntityManagerFactory emf;

    private EM() {
    }    

    public static synchronized void init(String persistenceUnit) {
        if (emf == null) {
            emf = Persistence.createEntityManagerFactory(
                    persistenceUnit);
        }
    }

    public static synchronized void init(String persistenceUnit, Map parameters) {
        if (emf == null) {
            emf = Persistence.createEntityManagerFactory(
                    persistenceUnit, parameters);
        }
    }

    public static synchronized EntityManager getInstance() {
        return emf.createEntityManager();
    }
}
