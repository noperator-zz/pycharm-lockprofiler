// Copyright 2000-2022 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package nl.jusx.pycharm.lockprofiler.render;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import nl.jusx.pycharm.lockprofiler.profile.Profile;
import org.jetbrains.annotations.NotNull;

public class LockProfilerToolWindowFactory implements ToolWindowFactory, DumbAware {
  @Override
  public void init(@NotNull ToolWindow toolWindow) {
    toolWindow.setToHideOnEmptyContent(true);
  }


  /**
   * Create the tool window content.
   *
   * @param project    current project
   * @param toolWindow current tool window
   */
  public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
//    AnAction action = new DumbAwareAction(AllIcons.General.Add) {
//      @Override
//      public void actionPerformed(@NotNull AnActionEvent e) {
//        @NotNull ListPopup popup = JBPopupFactory.getInstance()
//                .createActionGroupPopup(null, new ConnectActionGroup(), e.getDataContext(), false, null, 5);
//        popup.showUnderneathOf(e.getInputEvent().getComponent());
//      }
//    };
//    ((ToolWindowEx)toolWindow).setTabActions(action);

    return;
//    LockProfilerToolWindow lockProfilerToolWindow = new LockProfilerToolWindow(toolWindow, project);
//    ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
//    Content content = contentFactory.createContent(lockProfilerToolWindow.getContent(), "Lock Profiler", false);
//    toolWindow.getContentManager().addContent(content);
  }

//  @Override
//  public boolean shouldBeAvailable(@NotNull Project project) {
//    return false;
//  }

//  public static void addTab(@NotNull Project project,
//                            @NotNull ToolWindow toolWindow,
//                            @NotNull @NlsSafe String name,
//                            @NotNull Profile profile) {
//
//
//    LockProfilerToolWindow lockProfilerToolWindow = new LockProfilerToolWindow(toolWindow, project, profile);
//    ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
//    Content content = contentFactory.createContent(lockProfilerToolWindow.getContent(), name, false);
//    toolWindow.getContentManager().addContent(content);
//
////    SimpleToolWindowPanel panel = new SimpleToolWindowPanel(false, true);
////    Content content = ContentFactory.SERVICE.getInstance().createContent(panel, "", true);
//    content.putUserData(ToolWindow.SHOW_CONTENT_ICON, Boolean.TRUE);
////    SerialMonitor serialMonitor = new SerialMonitor(project, icon -> content.setIcon(icon), name, portProfile);
////    panel.setContent(serialMonitor.getComponent());
//
////    content.setDisplayName(name);
//    content.setDisposer(lockProfilerToolWindow);
//    content.setCloseable(true);
//    toolWindow.getContentManager().addContent(content);
//    toolWindow.getContentManager().setSelectedContent(content, true);
//    toolWindow.setAvailable(true);
//    lockProfilerToolWindow.refresh();
//  }
}
