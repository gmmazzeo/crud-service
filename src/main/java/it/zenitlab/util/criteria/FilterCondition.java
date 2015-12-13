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

package it.zenitlab.util.criteria;

/**
 *
 * @author Giuseppe M. Mazzeo <gmmazzeo@gmail.com>
 * @author Michele Milidoni <michelemilidoni@gmail.com>
 */
public class FilterCondition {

    public static final int EQ = 0, NEQ = -3, LT = -2, LE = -1, GT = 2, GE = 1, LK = 3, BT = 4, NNL=5, NL=6, EMPTY=7, NEMPTY=-7;
    String attribute;
    int operator;
    Object operand, operand2;
    String operandClassName, operand2ClassName;
    boolean isCaseSensitive;

    public FilterCondition(String attribute, int operator, Object operand, boolean isCaseSensitive) {
        this.attribute = attribute;
        this.operator = operator;
        this.operand = operand;
        this.isCaseSensitive = !(operand instanceof String) || isCaseSensitive;
    }

    public FilterCondition(String attribute, int operator, Object operand, Object operand2, Boolean isCaseSensitive) {
        this.attribute = attribute;
        this.operator = operator;
        this.operand = operand;
        this.operand2 = operand2;
        this.isCaseSensitive = !(operand instanceof String) || isCaseSensitive;
    }
    
    public FilterCondition(String attribute, int operator, Object operand, String operandClassName, boolean isCaseSensitive) {
        this.attribute = attribute;
        this.operator = operator;
        this.operand = operand;
        this.operandClassName = operandClassName;
        this.isCaseSensitive = !(operand instanceof String) || isCaseSensitive;
    }

    public FilterCondition(String attribute, int operator, Object operand, Object operand2, String operandClassName, String operand2ClassName, Boolean isCaseSensitive) {
        this.attribute = attribute;
        this.operator = operator;
        this.operand = operand;
        this.operand2 = operand2;
        this.operandClassName = operandClassName;
        this.operand2ClassName = operand2ClassName;        
        this.isCaseSensitive = !(operand instanceof String) || isCaseSensitive;
    }    

    public String getAttribute() {
        return attribute;
    }

    public void setAttribute(String attribute) {
        this.attribute = attribute;
    }

    public boolean getIsCaseSensitive() {
        return isCaseSensitive;
    }

    public void setIsCaseSensitive(boolean isCaseSensitive) {
        this.isCaseSensitive = isCaseSensitive;
    }

    public Object getOperand() {
        return operand;
    }

    public void setOperand(Object operand) {
        this.operand = operand;
    }

    public Object getOperand2() {
        return operand2;
    }

    public void setOperand2(Object operand2) {
        this.operand2 = operand2;
    }

    public String getOperand2ClassName() {
        return operand2ClassName;
    }

    public void setOperand2ClassName(String operand2ClassName) {
        this.operand2ClassName = operand2ClassName;
    }

    public String getOperandClassName() {
        return operandClassName;
    }

    public void setOperandClassName(String operandClassName) {
        this.operandClassName = operandClassName;
    }    

    public int getOperator() {
        return operator;
    }

    public void setOperator(int operator) {
        this.operator = operator;
    }
}
