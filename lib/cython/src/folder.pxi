
#######################
# Folders
#

cdef Folder initFolder(FolderT& f):
    cdef Folder folder = Folder()
    folder.setFolder(f)
    return folder


cdef class Folder:

    cdef:
        FolderT folder
        TaskTotals _totals
        list _jobs

    def __init__(self):
        self._jobs = []
        self._totals = None

    def __repr__(self):
        return "<Folder: %s>" % self.name

    cdef setFolder(self, FolderT& f):
        cdef TaskTotalsT totals = self.folder.totals
        self.folder = f
        self._jobs = []
        self._totals = initTaskTotals(totals)

    property id:
        def __get__(self):
            return self.folder.id

    property name:
        def __get__(self):
            return self.folder.name

    property minCores:
        def __get__(self):
            return self.folder.minCores

    property maxCores:
        def __get__(self):
            return self.folder.maxCores

    property runCores:
        def __get__(self):
            return self.folder.runCores

    property order:
        def __get__(self):
            return self.folder.order

    property totals:
        def __get__(self):
            cdef TaskTotalsT totals

            if self._totals is None:
                totals = self.folder.totals
                result = initTaskTotals(totals)
                self._totals = result

            return self._totals

    property jobs:
        def __get__(self):
            cdef JobT jobT

            if not self._jobs:
                self._jobs = [initJob(jobT) for jobT in self.folder.jobs]

            return self._jobs


def get_folder(Guid& folderId):
    cdef:
        FolderT folderT
        Folder folder 

    getClient().proxy().getFolder(folderT, folderId)
    folder = initFolder(folderT)
    return folder

def get_folders(Guid& projectId):
    cdef Project proj = get_project_by_id(projectId)
    cdef list folders = Project.get_folders(proj)
    return

def create_folder(Guid& projectId, string name):
    cdef FolderT folderT 
    getClient().proxy().createFolder(folderT, projectId, name)
    cdef Folder folder = initFolder(folderT)
    return folder

def get_job_board(Guid& projectId):
    cdef: 
        FolderT folderT 
        vector[FolderT] folders

    getClient().proxy().getJobBoard(folders, projectId)
    cdef list ret = [initFolder(folderT) for folderT in folders]
    return ret


