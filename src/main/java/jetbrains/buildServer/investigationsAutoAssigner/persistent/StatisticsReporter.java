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

public class StatisticsReporter {
  private final StatisticsDao myStatisticsDao;
  private Statistics myStatistics;

  public StatisticsReporter(StatisticsDao statisticsDao) {
    myStatistics = statisticsDao.read();
    myStatisticsDao = statisticsDao;
  }

  public void reportShownButton() {
    myStatistics.shownButtonsCount++;
    myStatisticsDao.write(myStatistics);
  }

  public void reportClickedButton() {
    myStatistics.clickedButtonsCount++;
    myStatisticsDao.write(myStatistics);
  }

  public void reportAssignedInvestigation() {
    myStatistics.assignedInvestigationsCount++;
    myStatisticsDao.write(myStatistics);
  }

  public void reportWrongInvestigation() {
    myStatistics.wrongInvestigationsCount++;
    myStatisticsDao.write(myStatistics);
  }
}

