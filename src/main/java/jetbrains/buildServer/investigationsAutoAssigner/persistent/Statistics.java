/*
 * Copyright 2000-2019 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jetbrains.buildServer.investigationsAutoAssigner.persistent;

import jetbrains.buildServer.investigationsAutoAssigner.common.Constants;

class Statistics implements Cloneable {

  private final String version;
  private int shownButtonsCount;
  private int clickedButtonsCount;
  private int assignedInvestigationsCount;
  private int wrongInvestigationsCount;

  public String getVersion() {
    return version;
  }

  int getShownButtonsCount() {
    return shownButtonsCount;
  }

  int getClickedButtonsCount() {
    return clickedButtonsCount;
  }

  int getAssignedInvestigationsCount() {
    return assignedInvestigationsCount;
  }

  int getWrongInvestigationsCount() {
    return wrongInvestigationsCount;
  }

  Statistics() {
    version = Constants.STATISTICS_FILE_VERSION;
  }

  void increaseShownButtonsCounter() {
    shownButtonsCount++;
  }

  void increaseClickedButtonsCounter() {
    clickedButtonsCount++;
  }

  void increaseAssignedInvestigationsCounter(final int count) {
    assignedInvestigationsCount += count;
  }

  void increaseWrongInvestigationsCounter(final int count) {
    wrongInvestigationsCount += count;
  }

  @Override
  public boolean equals(final Object obj) {
    if (!(obj instanceof Statistics)) {
      return false;
    }

    Statistics another = (Statistics)obj;
    return version.equals(another.version) &&
           shownButtonsCount == another.shownButtonsCount &&
           clickedButtonsCount == another.clickedButtonsCount &&
           assignedInvestigationsCount == another.assignedInvestigationsCount &&
           wrongInvestigationsCount == another.wrongInvestigationsCount;

  }

  @Override
  protected Statistics clone() {
    Statistics newStatisticsObj = new Statistics();
    newStatisticsObj.shownButtonsCount = shownButtonsCount;
    newStatisticsObj.clickedButtonsCount = clickedButtonsCount;
    newStatisticsObj.assignedInvestigationsCount = assignedInvestigationsCount;
    newStatisticsObj.wrongInvestigationsCount = wrongInvestigationsCount;
    return newStatisticsObj;
  }
}
