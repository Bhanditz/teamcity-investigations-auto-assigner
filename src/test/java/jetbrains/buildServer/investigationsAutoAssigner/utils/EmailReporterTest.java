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

package jetbrains.buildServer.investigationsAutoAssigner.utils;

import java.util.Arrays;
import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.investigationsAutoAssigner.common.Constants;
import jetbrains.buildServer.investigationsAutoAssigner.common.FailedBuildInfo;
import jetbrains.buildServer.investigationsAutoAssigner.common.HeuristicResult;
import jetbrains.buildServer.investigationsAutoAssigner.common.Responsibility;
import jetbrains.buildServer.investigationsAutoAssigner.persistent.StatisticsReporter;
import jetbrains.buildServer.parameters.ParametersProvider;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.users.User;
import jetbrains.buildServer.util.EmailSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Test
public class EmailReporterTest extends BaseTestCase {

  private EmailReporter myEmailReporter;
  private EmailSenderMock myMockedEmailSender;
  private BuildEx mySBuildMock;
  private Responsibility myResponsibility1;
  private Responsibility myResponsibility2;
  private CustomParameters myCustomParameters;
  private String TEST_LINK_URL = "testLinkUrl.com";
  private Long BUILD_ID = 239L;
  private String BUILD_TYPE_NAME = "testBuildTypeName";
  private HeuristicResult myHeuristicResult;
  private FailedBuildInfo myFailedBuildInfo;
  private WebLinks myWebLinks;
  private StatisticsReporter myStatisticsReporterMock;
  private String myStatisticReport;

  @BeforeMethod
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    myMockedEmailSender = new EmailSenderMock();
    myWebLinks = Mockito.mock(WebLinks.class);
    myStatisticsReporterMock = Mockito.mock(StatisticsReporter.class);
    mySBuildMock = Mockito.mock(BuildEx.class);
    final STestRun sTestRun1 = Mockito.mock(STestRun.class);
    final STestRun sTestRun2 = Mockito.mock(STestRun.class);
    final STest sTest1 = Mockito.mock(STest.class);
    final STest sTest2 = Mockito.mock(STest.class);
    myCustomParameters = Mockito.mock(CustomParameters.class);
    final User user1 = Mockito.mock(User.class);
    final User user2 = Mockito.mock(User.class);
    final BuildStatistics buildStatistics = Mockito.mock(BuildStatistics.class);
    final ParametersProvider parametersProvider = Mockito.mock(ParametersProvider.class);
    myResponsibility1 = new Responsibility(user1, "testDescription");
    myResponsibility2 = new Responsibility(user2, "testDescription2");


    myStatisticReport = "239__239";
    when(mySBuildMock.getBuildStatistics(any())).thenReturn(buildStatistics);
    when(buildStatistics.getFailedTests()).thenReturn(Arrays.asList(sTestRun1, sTestRun2));
    when(user1.getUsername()).thenReturn("testUser1");
    when(user2.getUsername()).thenReturn("testUser2");
    when(sTestRun1.getTest()).thenReturn(sTest1);
    when(sTestRun2.getTest()).thenReturn(sTest2);
    when(sTestRun1.getTestRunId()).thenReturn(1);
    when(sTestRun2.getTestRunId()).thenReturn(2);
    when(sTest1.getTestNameId()).thenReturn(1L);
    when(sTest2.getTestNameId()).thenReturn(2L);
    when(mySBuildMock.getBuildId()).thenReturn(BUILD_ID);
    when(mySBuildMock.getBuildTypeName()).thenReturn(BUILD_TYPE_NAME);
    when(mySBuildMock.getParametersProvider()).thenReturn(parametersProvider);
    myHeuristicResult = new HeuristicResult();
    myHeuristicResult.addResponsibility(sTestRun1, myResponsibility1);
    myHeuristicResult.addResponsibility(sTestRun2, myResponsibility2);

    String testEmail = "test.mail.com";
    when(myCustomParameters.getEmailForEmailReporter()).thenReturn(testEmail);
    when(myWebLinks.getViewResultsUrl(mySBuildMock)).thenReturn(TEST_LINK_URL);
    when(parametersProvider.get(Constants.SHOULD_DELAY_ASSIGNMENTS)).thenReturn("true");
    myFailedBuildInfo = new FailedBuildInfo(mySBuildMock);

    myEmailReporter = new EmailReporter(myMockedEmailSender, myWebLinks, myCustomParameters, myStatisticsReporterMock);
  }

  public void TestEmailAddressAbsents() {
    when(myCustomParameters.getEmailForEmailReporter()).thenReturn(null);
    myEmailReporter = new EmailReporter(myMockedEmailSender, myWebLinks, myCustomParameters, myStatisticsReporterMock);

    myFailedBuildInfo.addHeuristicsResult(myHeuristicResult);
    myEmailReporter.sendResults(myFailedBuildInfo);

    assertFalse(myMockedEmailSender.called);
  }

  public void TestNoResponsibilities() {
    myFailedBuildInfo.addHeuristicsResult(new HeuristicResult());

    myEmailReporter.sendResults(myFailedBuildInfo);

    assertFalse(myMockedEmailSender.called);
  }

  public void TestEmailAddress() {
    String testEmail = "test.mail.com";
    when(myCustomParameters.getEmailForEmailReporter()).thenReturn(testEmail);

    myFailedBuildInfo.addHeuristicsResult(myHeuristicResult);
    myEmailReporter.sendResults(myFailedBuildInfo);

    assertTrue(myMockedEmailSender.called);
    assertEquals(testEmail, myMockedEmailSender.usedAddress);
  }

  public void TestTopicContainsBuildLink() {
    myFailedBuildInfo.addHeuristicsResult(myHeuristicResult);
    myEmailReporter.sendResults(myFailedBuildInfo);

    assertTrue(myMockedEmailSender.called);
    assertTrue(myMockedEmailSender.usedSubject.contains(mySBuildMock.getBuildId() + ""));
  }

  public void TestCompareWithGold() {
    when(myStatisticsReporterMock.generateReport()).thenReturn(myStatisticReport);
    myFailedBuildInfo.addHeuristicsResult(myHeuristicResult);
    myEmailReporter.sendResults(myFailedBuildInfo);

    assertTrue(myMockedEmailSender.called);
    assertEquals(getHtmlReportGold(), myMockedEmailSender.usedHtml);
  }

  private String getHtmlReportGold() {
    return String.format("<!DOCTYPE html>\n" +
                         "<html>\n" +
                         "<body>\n" +
                         "<h2>Report for <a href=\"%s\">%s#%s</a>. Found 2 investigations:</h2>\n" +
                         "<ol>\n<li><a href=\"testLinkUrl.com#testNameId1\">Investigation</a> was assigned to %s who %s.</li>\n" +
                         "<li><a href=\"testLinkUrl.com#testNameId2\">Investigation</a> was assigned to %s who %s.</li>\n" +
                         "</ol>\n" +
                         "%s\n" +
                         "</body>\n" +
                         "</html>",
                         TEST_LINK_URL,
                         BUILD_TYPE_NAME,
                         BUILD_ID,
                         myResponsibility1.getUser().getUsername(),
                         myResponsibility1.getDescription(),
                         myResponsibility2.getUser().getUsername(),
                         myResponsibility2.getDescription(),
                         myStatisticReport);
  }

  private class EmailSenderMock implements EmailSender {

    boolean called = false;
    String usedAddress;
    String usedSubject;
    String usedHtml;

    @Override
    public void send(@NotNull final String address,
                     @NotNull final String subject,
                     @NotNull final String plainText,
                     @Nullable final String html) {
      called = true;
      usedAddress = address;
      usedSubject = subject;
      usedHtml = html;
    }
  }
}

