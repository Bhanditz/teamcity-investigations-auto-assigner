/*
 * Copyright 2000-2018 JetBrains s.r.o.
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

package jetbrains.buildServer.investigationsAutoAssigner.heuristics;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.investigationsAutoAssigner.common.HeuristicResult;
import jetbrains.buildServer.investigationsAutoAssigner.processing.HeuristicContext;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.STestRun;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.users.UserSet;
import jetbrains.buildServer.vcs.SelectPrevBuildPolicy;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.Mockito.when;

@Test
public class OneCommitterHeuristicTest extends BaseTestCase {

  private OneCommitterHeuristic myHeuristic;
  private UserSet<SUser> myUserSetMock;
  private SUser myFirstUser;
  private SUser mySecondUser;
  private STestRun mySTestRun;
  private HeuristicContext myHeuristicContext;
  private SProject mySProject;
  private SBuild mySBuild;

  @BeforeMethod
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    myHeuristic = new OneCommitterHeuristic();
    mySBuild = Mockito.mock(SBuild.class);
    mySProject = Mockito.mock(SProject.class);

    myUserSetMock = Mockito.mock(UserSet.class);
    myFirstUser = Mockito.mock(SUser.class);
    mySecondUser = Mockito.mock(SUser.class);
    when(mySBuild.getCommitters(SelectPrevBuildPolicy.SINCE_LAST_BUILD)).thenReturn(myUserSetMock);
    mySTestRun = Mockito.mock(STestRun.class);
    myHeuristicContext = new HeuristicContext(mySBuild,
                                              mySProject,
                                              Collections.emptyList(),
                                              Collections.singletonList(mySTestRun),
                                              Collections.emptyList());
    when(myFirstUser.getUsername()).thenReturn("myFirstUser");
    when(mySecondUser.getUsername()).thenReturn("mySecondUser");
  }

  public void TestWithOneResponsible() {
    when(myUserSetMock.getUsers()).thenReturn(new HashSet<>(Collections.singletonList(myFirstUser)));
    HeuristicResult heuristicResult = myHeuristic.findResponsibleUser(myHeuristicContext);

    Assert.assertFalse(heuristicResult.isEmpty());
    Assert.assertNotNull(heuristicResult.getResponsibility(mySTestRun));
    Assert.assertEquals(heuristicResult.getResponsibility(mySTestRun).getUser(), myFirstUser);
  }

  public void TestWithoutResponsible() {
    when(myUserSetMock.getUsers()).thenReturn(new HashSet<>());

    HeuristicResult heuristicResult = myHeuristic.findResponsibleUser(myHeuristicContext);

    Assert.assertTrue(heuristicResult.isEmpty());
  }

  public void TestWithManyResponsible() {
    when(myUserSetMock.getUsers()).thenReturn(new HashSet<>(Arrays.asList(myFirstUser, mySecondUser)));

    HeuristicResult heuristicResult = myHeuristic.findResponsibleUser(myHeuristicContext);

    Assert.assertTrue(heuristicResult.isEmpty());
  }

  public void TestWhiteList() {
    when(myUserSetMock.getUsers()).thenReturn(new HashSet<>(Arrays.asList(myFirstUser, mySecondUser)));
    HeuristicContext hc = new HeuristicContext(mySBuild,
                                               mySProject,
                                               Collections.emptyList(),
                                               Collections.singletonList(mySTestRun),
                                               Collections.singletonList(myFirstUser.getUsername()));
    HeuristicResult heuristicResult = myHeuristic.findResponsibleUser(hc);

    Assert.assertFalse(heuristicResult.isEmpty());
  }
}
