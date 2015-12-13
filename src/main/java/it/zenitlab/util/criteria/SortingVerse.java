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
public class SortingVerse {

    public static final int ASC = 1, DESC = -1;
    int versus;
    String attribute;
    boolean isCaseSensitive;

    public SortingVerse(String attribute, int versus, boolean isCaseSensitive) {
        this.attribute = attribute;
        this.versus = versus;
        this.isCaseSensitive = isCaseSensitive;
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

    public int getVersus() {
        return versus;
    }

    public void setVersus(int versus) {
        this.versus = versus;
    }
}
