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

package it.zenitlab.crudservice.exception;

import java.util.ArrayList;

/**
 *
 * @author Giuseppe M. Mazzeo <gmmazzeo@gmail.com>
 * @author Michele Milidoni <michelemilidoni@gmail.com>
 */
public class DependingObjectsException extends ServiceException {
    Class classOfDependingObjects;
    ArrayList<Object> idOfDependingObjects;
    
    public DependingObjectsException(String message, Class classOfDependingObjects, Object idOfDependingObject) {
        super(ServiceException.DEPENDING_OBJECTS, message, message);
        this.classOfDependingObjects=classOfDependingObjects;
        idOfDependingObjects=new ArrayList<Object>();
        idOfDependingObjects.add(idOfDependingObject);
    }

    public DependingObjectsException(String message, Class classOfDependingObjects, ArrayList<Object> idOfDependingObjects) {
        super(ServiceException.DEPENDING_OBJECTS, message, message);
        this.classOfDependingObjects=classOfDependingObjects;
        idOfDependingObjects=new ArrayList<Object>();
        this.idOfDependingObjects=idOfDependingObjects;
    }
    
    public Class getClassOfDependingObjects() {
        return classOfDependingObjects;
    }

    public ArrayList<Object> getIdOfDependingObjects() {
        return idOfDependingObjects;
    }
}
