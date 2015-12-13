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

/**
 *
 * @author Giuseppe M. Mazzeo <gmmazzeo@gmail.com>
 * @author Michele Milidoni <michelemilidoni@gmail.com>
 */
public class ServiceException extends Exception {

    String detailedMessage, userMessage;
    int code;
    
    public final static int GENERIC_ERROR=1000, INVALID_CLASS=1001, MISSING_PARAMETER=1002, INVALID_PARAMETER=1003, DEPENDING_OBJECTS=1004;
    
    public ServiceException(String detailedMessage, String userMessage) {
        this.code = GENERIC_ERROR;
        this.detailedMessage=detailedMessage;
        this.userMessage=userMessage;
    }    
    
    public ServiceException(String userMessage) {
        this.code = GENERIC_ERROR;
        this.detailedMessage=userMessage;
        this.userMessage=userMessage;
    }        

    public ServiceException(int code, String detailedMessage, String userMessage) {
        this.code = code;
        this.detailedMessage=detailedMessage;
        this.userMessage=userMessage;
    }

    public int getCode() {
        return code;
    }

    public String getDetailedMessage() {
        return detailedMessage;
    }

    public String getUserMessage() {
        return userMessage;
    }

    @Override
    public String getMessage() {
        return userMessage;
    }    
}
