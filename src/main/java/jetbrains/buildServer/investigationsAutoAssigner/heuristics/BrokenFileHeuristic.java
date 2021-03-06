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

import com.intellij.openapi.diagnostic.Logger;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;
import jetbrains.buildServer.investigationsAutoAssigner.processing.HeuristicContext;
import jetbrains.buildServer.investigationsAutoAssigner.common.HeuristicResult;
import jetbrains.buildServer.investigationsAutoAssigner.common.Responsibility;
import jetbrains.buildServer.investigationsAutoAssigner.utils.ProblemTextExtractor;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.problems.BuildProblem;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.vcs.SVcsModification;
import jetbrains.buildServer.vcs.SelectPrevBuildPolicy;
import jetbrains.buildServer.vcs.VcsFileModification;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.intellij.openapi.util.text.StringUtil.join;

public class BrokenFileHeuristic implements Heuristic {

  private static final Logger LOGGER = Logger.getInstance(BrokenFileHeuristic.class.getName());
  private static final int SMALL_PATTERN_THRESHOLD = 15;
  private final ProblemTextExtractor myProblemTextExtractor;

  public BrokenFileHeuristic(ProblemTextExtractor problemTextExtractor) {
    myProblemTextExtractor = problemTextExtractor;
  }

  @Override
  @NotNull
  public String getName() {
    return "Detect Broken File Heuristic";
  }

  public HeuristicResult findResponsibleUser(@NotNull HeuristicContext heuristicContext) {
    HeuristicResult result = new HeuristicResult();
    SBuild sBuild = heuristicContext.getBuild();

    final BuildPromotion buildPromotion = sBuild.getBuildPromotion();
    if (!(buildPromotion instanceof BuildPromotionEx)) return result;

    SelectPrevBuildPolicy prevBuildPolicy = SelectPrevBuildPolicy.SINCE_LAST_BUILD;
    List<SVcsModification> vcsChanges = ((BuildPromotionEx)buildPromotion).getDetectedChanges(prevBuildPolicy, false)
                                                                          .stream()
                                                                          .map(ChangeDescriptor::getRelatedVcsChange)
                                                                          .filter(Objects::nonNull)
                                                                          .collect(Collectors.toList());
    for (STestRun sTestRun : heuristicContext.getTestRuns()) {
      String problemText = myProblemTextExtractor.getBuildProblemText(sTestRun);
      Responsibility responsibility =
        findResponsibleUser(vcsChanges, sBuild, problemText, heuristicContext.getUserFilter());
      if (responsibility != null)
        result.addResponsibility(sTestRun, responsibility);
    }

    for (BuildProblem buildProblem : heuristicContext.getBuildProblems()) {
      String problemText = myProblemTextExtractor.getBuildProblemText(buildProblem, sBuild);
      Responsibility responsibility =
        findResponsibleUser(vcsChanges, sBuild, problemText, heuristicContext.getUserFilter());
      if (responsibility != null)
        result.addResponsibility(buildProblem, responsibility);
    }

    return result;
  }

  @Nullable
  private Responsibility findResponsibleUser(List<SVcsModification> vcsChanges,
                                             SBuild sBuild,
                                             String problemText,
                                             List<String> usernamesBlackList) {
    SUser responsibleUser = null;
    String brokenFile = null;
    for (SVcsModification vcsChange : vcsChanges) {
      final String foundBrokenFile = findBrokenFile(vcsChange, problemText);
      if (foundBrokenFile == null) continue;

      final Collection<SUser> changeCommitters = vcsChange.getCommitters()
                                                          .stream()
                                                          .filter(user->!usernamesBlackList.contains(user.getUsername()))
                                                          .collect(Collectors.toList());
      if (changeCommitters.size() == 0) continue;
      if (changeCommitters.size() > 1) return null;

      final SUser foundResponsibleUser = changeCommitters.iterator().next();
      if (responsibleUser != null && !responsibleUser.equals(foundResponsibleUser)) {
        LOGGER.debug(String.format("Build %s: There are more than one committer since last build",
                                   sBuild.getBuildId()));
        return null;
      }
      responsibleUser = foundResponsibleUser;
      brokenFile = foundBrokenFile;
    }

    if (responsibleUser == null) return null;

    String description = String.format("changed the suspicious file \"%s\" which probably broke the build", brokenFile);
    return new Responsibility(responsibleUser, description);
  }

  @Nullable
  private static String findBrokenFile(@NotNull final SVcsModification vcsChange, @NotNull final String problemText) {
    for (VcsFileModification modification : vcsChange.getChanges()) {
      final String filePath = modification.getRelativeFileName();
      for (String pattern : getPatterns(filePath)) {
        if (problemText.contains(pattern)) {
          return filePath;
        }
      }
    }
    return null;
  }

  /**
   * This method is required to separate path1/path2/fileName with path3/path4/fileName.
   * Also it allows to handle different separators. Currently supported: '.','/','\' separators.
   * @param filePath - filePath of the modification
   * @return various combination of fileName and its parents(up to 2th level) with separators.
   */
  @NotNull
  private static List<String> getPatterns(@NotNull final String filePath) {
    final List<String> parts = new ArrayList<>();
    String withoutExtension = FileUtil.getNameWithoutExtension(new File(filePath));
    if (withoutExtension.length() == 0) {
      return Collections.emptyList();
    }
    parts.add(withoutExtension);

    String path = getParentPath(filePath);
    if (path != null) {
      parts.add(0, new File(path).getName());
      path = getParentPath(path);
      if (path != null) {
        parts.add(0, new File(path).getName());
      }
    }

    if (isSmallPattern(parts)) {
      String withExtension = FileUtil.getName(filePath);
      parts.set(0, withExtension);
    }

    return parts.isEmpty() ?
           Collections.emptyList() :
           Arrays.asList(join(parts, "."), join(parts, "/"), join(parts, "\\"));
  }

  private static boolean isSmallPattern(final List<String> parts) {
    return join(parts, ".").length() <= SMALL_PATTERN_THRESHOLD;
  }

  // we do not use File#getParentFile() instead because we must not take current
  // working directory into account, i.e. getParentPath("abc") must return null
  @Nullable
  private static String getParentPath(@NotNull final String path) {
    final int lastSlashPos = path.replace('\\', '/').lastIndexOf('/');
    return lastSlashPos == -1 ? null : path.substring(0, lastSlashPos);
  }
}

