"""
The render job watch panel allows you to 
   1. setup filters to automatically load jobs. (defaults to loading your jobs)
   2. individually add jobs you want to watch.
"""
import os
import plow.client

from plow.gui.manifest import QtCore, QtGui
from plow.gui.panels import Panel
from plow.gui.common.widgets import CheckableListBox, BooleanCheckBox, SpinSliderWidget
from plow.gui.common.job import JobProgressBar, JobSelectionDialog, JobStateWidget
from plow.gui.constants import COLOR_JOB_STATE
from plow.gui.util import formatMaxValue, formatDateTime
from plow.gui.event import EventManager

class RenderJobWatchPanel(Panel):

    def __init__(self, name="Render Watch", parent=None):
        Panel.__init__(self, name, "Render Watch", parent)

        self.setAttr("loadMine", True)
        self.setAttr("projects", [])
        self.setAttr("allProjects", True)
        self.setAttr("refreshSeconds", 10)

        self.setWidget(RenderJobWatchWidget(self.attrs, self))
        self.setWindowTitle(name)
        self.refresh()
        self.setRefreshTime(self.attrs["refreshSeconds"])

    def init(self):
        # TODO
        # sweep button (remove finished)
        # refresh button
        # seperator
        # kill button (multi-select)
        # comment button (multi-select)
        # 
        self.titleBarWidget().addAction(
            QtGui.QIcon(":/load.png"), "Load", self.openLoadDialog)

    def openLoadDialog(self):
        dialog = JobSelectionDialog()
        if dialog.exec_():
            print "foo"


    def _openPanelSettingsDialog(self):
        d = RenderJobWatchSettingsDialog(self.attrs)
        if d.exec_():
            self.attrs.update(d.getAttrs())
            self.setRefreshTime(self.attrs["refreshSeconds"])

    def refresh(self):
        self.widget().refresh()

class RenderJobWatchWidget(QtGui.QWidget):

    Header = ["Job", "State", "Run", "Wait", "Min", "Max", "Started", "Stopped", "Progress"]
    Width = [400, 75, 60, 60, 60, 60, 100, 100, 250]

    def __init__(self, attrs, parent=None):
        QtGui.QWidget.__init__(self, parent)
        QtGui.QVBoxLayout(self)
        self.attrs = attrs
        self.__jobs = { }

        self.__tree = QtGui.QTreeWidget(self)
        self.__tree.setHeaderLabels(self.Header)
        self.__tree.setColumnCount(len(self.Header))
        self.__tree.setUniformRowHeights(True)
        self.__tree.viewport().setFocusPolicy(QtCore.Qt.NoFocus)
        [self.__tree.setColumnWidth(i, v) for i, v in enumerate(self.Width)]
        self.__tree.itemDoubleClicked.connect(self.__itemDoubleClicked)

        self.layout().addWidget(self.__tree)

    def refresh(self):
        jobs = plow.client.get_jobs(user=[os.environ["USER"]])       
        for job in jobs:
            if not self.__jobs.has_key(job.id):
                self.addJob(job)
            else:
                self.updateJob(job)

    def addJob(self, job):
        item = QtGui.QTreeWidgetItem([
            job.name,
            plow.client.JobState._VALUES_TO_NAMES[job.state].lower(),
            "%02d" % job.totals.runningTaskCount,
            "%02d" % job.totals.waitingTaskCount,
            "%02d" % job.minCores,
            formatMaxValue(job.maxCores),
            formatDateTime(job.startTime),
            formatDateTime(job.stopTime)])

        item.setData(0, QtCore.Qt.UserRole, job.id)
        self.__tree.addTopLevelItem(item)

        progress = JobProgressBar(job.totals, self.__tree)
        self.__tree.setItemWidget(item, len(self.Header)-1, progress);
        self.__tree.setItemWidget(item, 1, JobStateWidget(job.state, self))

        self.__jobs[job.id] = item

    def updateJob(self, job):
        item = self.__jobs[job.id]
        item.setText(2, "%02d" % job.totals.runningTaskCount)
        item.setText(3, "%02d" % job.totals.waitingTaskCount)
        item.setText(4, "%02d" % job.minCores)
        item.setText(5, formatMaxValue(job.maxCores))
        item.setText(7, formatDateTime(job.stopTime))
        self.__tree.itemWidget(item, len(self.Header)-1).setTotals(job.totals)
        self.__tree.itemWidget(item, 1).setState(job.state)

    def __itemDoubleClicked(self, item, col):
        uid = item.data(0, QtCore.Qt.UserRole)
        EventManager.emit("JOB_OF_INTEREST", uid)

class RenderJobWatchSettingsDialog(QtGui.QDialog):
    """
    A dialog box that lets you configure how the render job widget.
    """
    def __init__(self, attrs, parent=None):
        QtGui.QDialog.__init__(self, parent)
        layout = QtGui.QVBoxLayout(self)

        self.sliderRefresh = SpinSliderWidget(1, 60, attrs["refreshSeconds"], self)
        self.sliderRefresh.slider.setTickInterval(5)
        self.sliderRefresh.slider.setTickPosition(QtGui.QSlider.TicksBelow)
        self.checkboxLoadMine = BooleanCheckBox(bool(attrs["loadMine"]))
        self.listUsers = QtGui.QListWidget(self)
        self.listUsers.setMaximumHeight(50)
        self.checkboxLoadErrors = QtGui.QCheckBox(self)

        self.listProjects = CheckableListBox("Projects", 
            ["test1", "test2"], attrs["projects"], bool(attrs["allProjects"]), self)

        group_box1 = QtGui.QGroupBox("Auto Load Jobs", self)

        form_layout1 = QtGui.QFormLayout(group_box1)
        form_layout1.addRow("Refresh", self.sliderRefresh)
        form_layout1.addRow("Load Mine:", self.checkboxLoadMine)
        form_layout1.addRow("Load User:", self.listUsers)
        form_layout1.addRow("Load With Errors:", self.checkboxLoadErrors)

        # move to project multi-select widget
        group_box2 = QtGui.QGroupBox("Filters", self)
        form_layout2 = QtGui.QFormLayout(group_box2)
        form_layout2.addRow("For Projects:", self.listProjects)

        buttons = QtGui.QDialogButtonBox(QtGui.QDialogButtonBox.Ok | QtGui.QDialogButtonBox.Cancel);
        buttons.accepted.connect(self.accept)
        buttons.rejected.connect(self.reject)

        layout.addWidget(group_box1)
        layout.addWidget(group_box2)
        layout.addWidget(buttons)

    def getAttrs(self):
        return {
            "refreshSeconds": self.sliderRefresh.value(),
            "loadMine": self.checkboxLoadMine.isChecked(),
            "projects": self.listProjects.getCheckedOptions(),
            "allProjects": self.listProjects.isAllSelected()
        }

