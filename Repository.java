package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.*;
import java.text.SimpleDateFormat;

import static gitlet.Utils.*;


/** Represents a gitlet repository.
 *
 *  @author areeya
 */
public class Repository extends SimpleDateFormat implements Serializable{

    /** The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /** The .gitlet directory. */
    public static final File GITLET_DIR = join(CWD, ".gitlet");

    public Commit mostRecentCommit;
    public HashMap<String, byte[]> stagingArea;
    public HashMap<String, byte[]> removalArea; //NEW
    public File saveStage;
    public File COMMIT_DIR = join(GITLET_DIR,".commits");
    public String headId;
    //keeps track of all the branches in the repository
    private HashMap<String, String> branches;

    private File BRANCHES_FILE;// = Utils.join(GITLET_DIR, ".branches"); //TODO: NEW--for saving branches
    //keeps track of currently checked out branches
    private String currentBranch;
    private String workingDir;

    public Repository() {

        //check if gitlet already exists
        if (!GITLET_DIR.exists()) {
            GITLET_DIR.mkdir();
            COMMIT_DIR.mkdir();
            BRANCHES_FILE = new File(GITLET_DIR,".branches");
        }
        init();
    }

    public void init() {
        //create a initial commit and serialize it and get the sha1 and then create a file with it
        mostRecentCommit = new Commit("initial commit", null, new Date(0), new HashMap<>(), new HashMap<>());
        byte[] serialized_initialCommit = Utils.serialize(mostRecentCommit);
        String initialCommit_ID = Utils.sha1(serialized_initialCommit);

        File initialCommit_File = Utils.join(COMMIT_DIR, initialCommit_ID);
        Utils.writeContents(initialCommit_File, serialized_initialCommit);
        headId = initialCommit_ID;

        // create main branch
        branches = new HashMap<>();
        branches.put("main",headId); //branches is hashmap that contains all existing branches and the commit id for the head of each branch
        currentBranch = "main";
        Utils.writeObject(BRANCHES_FILE,branches); // saves state of branches, will be overwritten later as new branches emerge

        //keep track of staging area
        saveStage = Utils.join(GITLET_DIR, "staging");
        stagingArea = new HashMap<>();
        removalArea = new HashMap<>(); //NEW
        Utils.writeContents(saveStage, Utils.serialize(stagingArea));
    }

    public void add(String fileToAdd) {
        // adding already added = overwriting previous entry with new contents
        // if current working version is identical to version in current commit,
        // do not add + remove from staging area if it is already there

        // create new path for the file being added to staging area
        File newPath = Utils.join(CWD, fileToAdd);


        // if the path doesn't exist, print out error message
        if (!newPath.exists()) {
            System.out.println("File does not exist."); //CHANGE: to match spec
            return;
        }

        byte[] fileContents = Utils.readContents(newPath);
        byte[] stagedContents = stagingArea.get(fileToAdd);

        if (stagedContents != null && Arrays.equals(fileContents, stagedContents)) {
            return;
        }

        Commit mostRecentCommit = getCommitById(headId);
        if (mostRecentCommit != null && mostRecentCommit.getTrackedFileContents().containsKey(fileToAdd)&&
                Arrays.equals(fileContents,mostRecentCommit.getTrackedFileContents().get(fileToAdd))) {
            stagingArea.remove(fileToAdd);
            return;
        }

        stagingArea.put(fileToAdd, fileContents);
        Utils.writeObject(saveStage, stagingArea);
    }

    public void commit(String message) {
        /*default: a commit has the same file contents as parent
        - files staged for addition and removal are the updates to the commit
        - staging area is cleared after commit
        - new commit becomes 'current commit' with head pointer now pointing at it

        //each commit is identified by sha-1 id which includes the file references
        //to its files, parent references, log message, and commit time*/

        //if no message inputted, prints error message
        if (message.isEmpty()) {
            System.out.println("Please enter a commit message.");
            return;
        }
        if (stagingArea.isEmpty() && removalArea.isEmpty()) {
            System.out.println("No changes added to the commit.");
            return;
        }

        //create new commit with last commit as parent
        Commit newCommit = new Commit(message, headId, new Date(),
                mostRecentCommit.trackedFileContents, mostRecentCommit.filesRemoved);


        for (String fileName: stagingArea.keySet()) {
            newCommit.trackedFileContents.put(fileName, stagingArea.get(fileName));
        }

        for (String fileName: removalArea.keySet()) {
            if (newCommit.trackedFileContents.containsKey(fileName)) {
                newCommit.trackedFileContents.remove(fileName);
            }
        }

        newCommit.filesRemoved = removalArea;
        byte[] newCommitSerialized = Utils.serialize(newCommit); //serializes the newCommit Commit object
        String newCommitId = Utils.sha1(newCommitSerialized); //creates a sha-1 id from the commit object
        File newCommitFile = Utils.join(COMMIT_DIR,newCommitId); //creates new path for newCommitId txt file
        Utils.writeObject(newCommitFile, newCommit); //writes the serialized commit into newCommitFile
        headId = newCommitId;
        mostRecentCommit = newCommit;
        branches.replace(currentBranch, headId); // updates headId of current branch

        //resets the saved staging area
        saveStage.delete();
        stagingArea.clear();

        //NEW: deletes removal area
        removalArea.clear();

    }

    public void printLog() { 
        Commit commitToPrint = mostRecentCommit;
        String commitToPrintId = headId;

        while (commitToPrintId != null) {
            printLogFormatter(commitToPrintId, commitToPrint);
            commitToPrintId = commitToPrint.getParentId();

            if (commitToPrintId != null) {
                File nextCommitFilePath = Utils.join(COMMIT_DIR, commitToPrintId);
                commitToPrint = Utils.readObject(nextCommitFilePath, Commit.class);
            }
        }
    }

    // Formats output for specified commit
    public void printLogFormatter(String commitToPrintId, Commit commitToPrint) {
        SimpleDateFormat friend = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z");
        System.out.println("===");
        System.out.println("commit " + commitToPrintId);
        Date time = commitToPrint.getTimestamp();
        System.out.println("Date: " + friend.format(time));
        System.out.println(commitToPrint.getMessage());
        System.out.println();
    }

    public void restore(String fileName) {
    /* Takes the version of the file as it exists in the head commit
     * and puts it in the working directory, overwriting the version
     * of the file that’s already there if there is one.
     * The new version of the file is not staged. */

        // calls other restore method with headId as first argument
        restore(headId, fileName);

    }
    public void restore(String commitId, String fileName) {
        /* Takes the version of the file as it exists in the commit
        * with the given id, and puts it in the working directory,
        * overwriting the version of the file that’s already there
        * if there is one. The new version of the file is not staged.
        */

        if (commitId.length() < 40) {
            commitId = abbrevSHAToFull(commitId);
        }

        File commitToRestorePath = Utils.join(COMMIT_DIR, commitId);
        if (commitToRestorePath.exists()) {
            Commit commitToRestore = Utils.readObject(commitToRestorePath, Commit.class);
            HashMap<String, byte[]> commitToRestoreContents = commitToRestore.getTrackedFileContents(); //how commit keeps track of files + content
            byte[] fileToRestoreContents = commitToRestoreContents.get(fileName); //getting files contents
            File currentFileToReplacePath = Utils.join(CWD,fileName); //file to restore in current working directory
            if (!currentFileToReplacePath.exists()) {
                System.out.println("File does not exist in that commit.");
            } else {
                Utils.writeContents(currentFileToReplacePath, fileToRestoreContents); //overwrites contents in CWD
            }
        } else {
            System.out.println("No commit with that id exists.");
        }
    }

    // helper method for shortened ids
    public String abbrevSHAToFull(String commitId) {

        for (String s: COMMIT_DIR.list()) {
            if (s.contains(commitId)) {
                commitId = s;
            }
        }
        return commitId;
    }

    public void rm(String fileName) {
        /* Unstage the file if it is currently staged for
         * addition. If the file is tracked in the current
         * commit, stage it for removal and remove the file
         * from the working directory if the user has not
         * already done so (do not remove it unless it is
         * tracked in the current commit).
         *
         * If the file is neither staged nor tracked by the
         * head commit, print the error message
         * "No reason to remove the file."
         *  */

        // if neither staged nor tracked by the head commit, prints error message
        File fileToDelete = Utils.join(CWD, fileName); // makes path for file to be deleted
        if (saveStage.exists()) {
            stagingArea = Utils.readObject(saveStage, HashMap.class);
        }
        File mostRecent = Utils.join(COMMIT_DIR, headId);
        Commit mostRecentCommit = Utils.readObject(mostRecent, Commit.class);
        if ((!stagingArea.containsKey(fileName)) &&
                (!mostRecentCommit.trackedFileContents.containsKey(fileName)) && fileToDelete.exists()) {
            System.out.println("No reason to remove the file.");
        }

        if(stagingArea.containsKey(fileName)) { //checks if file is in staging area
            stagingArea.remove(fileName); //if so, removes file from staging area
        }
        //checks if it was in the most recent commit
        if (mostRecentCommit.trackedFileContents.containsKey(fileName)) {
            //adds file to be tracked by removalArea
            removalArea.put(fileName,mostRecentCommit.trackedFileContents.get(fileName));


            if (fileToDelete.exists()) {
                //System.out.println("i'm deleting!");
                fileToDelete.delete();
            }
        }

    }

    public void printStatus() {
        System.out.println("=== Branches ===");

        List<String> branchList = new ArrayList();

        for(String b: branches.keySet()) {
            if (b.equals(currentBranch)) {
                b = "*" + b;
                //System.out.println("btw the current branch is: "+ currentBranch);
            }
            branchList.add(0, b);
        }

        for(String b: branchList) {
            System.out.println(b);
            //System.out.println();
        }

        System.out.println("\n=== Staged Files ===");

        List<String> stagedFiles = new ArrayList<>(stagingArea.keySet());
        // Sort the list of filenames
        Collections.sort(stagedFiles);
        // Print the sorted filenames
        for (String fileName : stagedFiles) {
            System.out.println(fileName);
        }
        System.out.println("\n=== Removed Files ===");

        // Convert the keys in the removal area to a list
        List<String> removedFiles = new ArrayList<>(removalArea.keySet());
        // Sort the list of filenames
        Collections.sort(removedFiles);
        // Print the sorted filenames

        for (String fileName : removedFiles) {
            File inCWD = Utils.join(CWD, fileName);
            if (!inCWD.exists()) {
                System.out.println(fileName);
            }
        }

        System.out.println("\n=== Modifications Not Staged For Commit ===");
        List<String> modsNotStaged = new ArrayList<>();
        List<String> untrackedFiles = new ArrayList<>();
        for (String file: CWD.list()) {
            //compare file contents from CWD to file contents in latest commit
            if (mostRecentCommit.trackedFileContents.containsKey(file)) {
                //if (mostRecentCommit.trackedFileContents.)
            }

        }
        System.out.println("\n=== Untracked Files ===");
        //System.out.println("current branch: " + currentBranch);
        //System.out.println("current files: " + Arrays.toString(CWD.list()));

    }



    public void printGlobalLog() {
        /* Like log, except displays information about all commits
        ever made. The order of the commits does not matter.
        * */
        // makes list of all commit file names in .commits folder
        List<String> allCommits = Utils.plainFilenamesIn(COMMIT_DIR);

        //iterates all the commits in the file and prints them
        //out accordingly
        for (String s: allCommits) {
            String commitToPrintId = s;
            File commitToPrintFile = Utils.join(COMMIT_DIR, commitToPrintId);
            Commit commie = Utils.readObject(commitToPrintFile, Commit.class);
            printLogFormatter(commitToPrintId, commie);
        }
    }
    public void find(String commitMessage) {
        //Store the ids of commits that have give commit message
        List<String> commits = new ArrayList<>();
        //acces all the files and read the stored object and deserialize it as commit object
        for(File file : COMMIT_DIR.listFiles()) {
            Commit c = Utils.readObject(file, Commit.class);
            //checks if its equal to any other commit message and then add
            if (c.getMessage().equals(commitMessage)) {
                commits.add(file.getName());
            }
        }
        //check if its empty if not then go through all the commits and print the id
        if (commits.isEmpty()) {
            System.out.println("Found no commit with that message.");
        } else {
            for (String id : commits) {
                System.out.println(id);
            }
        }
    }

    public void branch(String branchName) {
        if (branches.containsKey(branchName)) {
            System.out.println("A branch with that name already exists.");
        } else {
            // Create a new branch and point it to the current head commit
            branches.put(branchName, headId);
            Utils.writeObject(BRANCHES_FILE, branches);
        }
    }

    public Commit getCommitById(String id) {
        File commitFile = new File(COMMIT_DIR, id);
        if (commitFile.exists()) {
            return Utils.readObject(commitFile, Commit.class);
        }
        return null;
    }
    public void switchBranch(String branchName) {
        if (!branches.containsKey(branchName)) {
            System.out.println("No such branch exists.");
            return;
        }
        if (currentBranch.equals(branchName)) {
            System.out.println("No need to switch to the current branch.");
            return;
        }
        // Get the target branch's head commit
        String targetBranchHeadId = branches.get(branchName);
        File targetBranchHeadCommitPath = Utils.join(COMMIT_DIR,targetBranchHeadId);
        Commit targetBranchHead = Utils.readObject(targetBranchHeadCommitPath, Commit.class); //returns the target commit

        //checks if a working file is untracked in the current branch and if
        //it would be overwritten by the switch--if so, prints out error message

        //untracked meaning it's changed and not been committed
        for (String file: CWD.list()) {
            mostRecentCommit = getCommitById(headId);
            if (targetBranchHead.trackedFileContents.containsKey(file) &&
                    (!mostRecentCommit.trackedFileContents.containsKey(file)) ) {
                System.out.println("There is an untracked file in the way; delete it," +
                        " or add and commit it first.");/* + "contents of CWD" + Arrays.asList(CWD.list()) +
                        "\n" + "contents of most recent commit: " + mostRecentCommit.trackedFileContents.keySet() + "\n" +
                        "files removed in most recent commit: " + mostRecentCommit.filesRemoved); // !!!!!!!*/
                return;
            }
        }

        // Overwrites contents of file in CWD if the same file exists in the
        // new branch's head commit, otherwise deletes the file if CWD
        // contains files not in new branch's head commit
        for (String fileName : CWD.list()) {
            if (targetBranchHead.trackedFileContents.containsKey(fileName)) {
                File inBoth = Utils.join(CWD, fileName);
                byte[] newContents = targetBranchHead.trackedFileContents.get(fileName);
                Utils.writeContents(inBoth, newContents);
            } else {
                Utils.restrictedDelete(Utils.join(CWD, fileName));
            }
        }
        //if file from target commit is not in CWD, adds it and its contents to CWD
        for (String fileName: targetBranchHead.trackedFileContents.keySet()) {
            File toAddToCWD = Utils.join(CWD, fileName);

            if (!toAddToCWD.exists()) {
                toAddToCWD = new File(CWD,fileName);
                Utils.writeContents(toAddToCWD, targetBranchHead.trackedFileContents.get(fileName));
            }
            //System.out.println(fileName);
        }

        // Clear the staging area
        stagingArea.clear();

        // Update the current branch and head commit
        currentBranch = branchName;
        headId = targetBranchHeadId;
        mostRecentCommit = targetBranchHead;
    }



    public void rmBranch(String branchName) {
        if (!branches.containsKey(branchName)) {
            System.out.println("A branch with that name does not exist.");
        } else if (currentBranch.equals(branchName)) {
            System.out.println("Cannot remove the current branch.");
        } else if (!stagingArea.isEmpty()) {
            System.out.println("Cannot remove the current branch.");
        } else {
            branches.remove(branchName);
            // Save the updated branches
            Utils.writeObject(BRANCHES_FILE, branches);
        }
    }

    /*private void saveBranches() {
        //File file = new File(GITLET_DIR, "branches");
        //!!!!
        Utils.writeObject(file, branches);
    }*/

    public void reset(String commitID) {
        /*Restores all the files tracked by the given commit.
        * Removes tracked files that are not present in that commit.
        * Also moves the current branch’s head to that commit node.
        * See the intro for an example of what happens to the head pointer after using reset.
        * The [commit id] may be abbreviated as for restore. The staging area is cleared.
        *
        *  If no commit with the given id exists, print No commit with that id exists.
        * If a working file is untracked in the current branch and would be overwritten by the reset,
        * print There is an untracked file in the way; delete it, or add and commit it first. and exit;
        * perform this check before doing anything else.
        * */
        if (commitID.length() < 40) {
            commitID = abbrevSHAToFull(commitID);
        }
        Commit commitToReset = getCommitById(commitID);
        mostRecentCommit = getCommitById(headId);
        // Check if commitID exists
        if (commitToReset == null) {
            System.out.println("No commit with that id exists.");
            return;
        }
        // Check for untracked files
        /*for (String file : CWD.list()) {
            if (!commitToReset.getTrackedFileContents().containsKey(file) &&
                    !stagingArea.containsKey(file) &&
                    mostRecentCommit.getTrackedFileContents().containsKey(file)) {
                System.out.println("Untracked files exist. Reset aborted");
                return;
            }
        }*/

        for (String file: CWD.list()) {
            File path = Utils.join(CWD, file);
            byte[] contents = {0};
            if (!path.isFile()) {
                break;
            } else {
                contents = Utils.readContents(path);
            }

            // if it exists in most recent commit and contents are different
            // if it exists in reset commit and contents are different
            // if it does not exist in most recent commit and does not exist in reset commit
            if ((mostRecentCommit.trackedFileContents.containsKey(file) &&
                    !Arrays.equals(mostRecentCommit.trackedFileContents.get(file), contents) && !mostRecentCommit.filesRemoved.containsKey(file)) ||
                    (!mostRecentCommit.trackedFileContents.containsKey(file) && !commitToReset.trackedFileContents.containsKey(file)) ||
                    (commitToReset.trackedFileContents.containsKey(file)&&!Arrays.equals(commitToReset.trackedFileContents.get(file), contents))) {
                System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
                return;
            }
        }



        // Update the working directory
        for (File file : CWD.listFiles()) {
            if (!commitToReset.getTrackedFileContents().containsKey(file.getName())) {
                file.delete();
            }
        }
        for (Map.Entry<String, byte[]> entry : commitToReset.getTrackedFileContents().entrySet()) {
            String file = entry.getKey();
            byte[] contents = entry.getValue();
            Utils.writeContents(new File(CWD, file), contents);
        }
        // Clear the staging area
        stagingArea.clear();
        // Update the head commit if the current branch is the main branch
        if (currentBranch.equals("main")) {
            headId = commitID;
        }
        // Update the branch's pointer to the new commit
        branches.put(currentBranch, commitID);
        Utils.writeObject(BRANCHES_FILE, branches);
        mostRecentCommit = commitToReset;
    }

    public void merge(String branchName) {

        if (!branches.containsKey(branchName)) {
            System.out.println("A branch with that name does not exist.");
            return;
        }
        Commit currentBranchPointer = getCommitById(headId);
        mostRecentCommit = getCommitById(headId);
        String otherBranchHeadCommitId = branches.get(branchName);
        Commit otherBranchPointer = getCommitById(branches.get(branchName));
        Set<String> currentBranchCommits = new HashSet<>();
        String splitPointId = null;
        boolean hadConflict = false;
        currentBranchCommits.add(headId);

        if (branchName.equals(currentBranch))  {
            System.out.println("Cannot merge a branch with itself.");
            return;
        }

        //iterates through all commits in current branch and adds them to a set
        while (currentBranchPointer.getParentId() != null) {
            currentBranchCommits.add(currentBranchPointer.getParentId());
            currentBranchPointer = getCommitById(currentBranchPointer.getParentId());
        }

        /*If an untracked file in the current commit would be overwritten or deleted by the merge,
        print There is an untracked file in the way; delete it, or add and commit it first. and exit;
        perform this check before doing anything else.
        * */


        //if the head id of the other branch is part of the current branch
        //commits, then it is the current commit or ancestor, thus printing
        //out the message as indicated in spec
        if (currentBranchCommits.contains(otherBranchHeadCommitId)) {
            System.out.println("Given branch is an ancestor of the current branch.");
            return;
        }

        //iterates through the commits of the other branch and compares them
        //to the set of commits of the current branch to find split point id
        while (otherBranchPointer.getParentId() != null) {
            if (currentBranchCommits.contains(otherBranchPointer.getParentId())) {
                splitPointId = otherBranchPointer.getParentId();
                break;
            }
            otherBranchPointer = getCommitById(otherBranchPointer.parentId);
        }

        // if the split point is the same as the current branch's head id
        // then fast-forward the current branch and print corresponding msg
        if (headId == splitPointId) {
            System.out.print("Current branch fast-forwarded.");

            return;
        }

        Commit splitPointCommit = getCommitById(splitPointId);
        Commit otherBranchCommit = getCommitById(otherBranchHeadCommitId);
        HashMap<String, byte[]> otherBranchFiles = otherBranchCommit.trackedFileContents;
        HashMap<String, byte[]> splitPointFiles = splitPointCommit.trackedFileContents;

        Set<String> allFileNames = new HashSet<>();
        allFileNames.addAll(splitPointFiles.keySet());
        allFileNames.addAll(otherBranchFiles.keySet());
        allFileNames.addAll(getCommitById(headId).trackedFileContents.keySet());

        if (!stagingArea.isEmpty() || !removalArea.isEmpty()) {
            System.out.println("You have uncommitted changes.");
            return;
        }

        /*If an untracked file in the current commit would be overwritten or deleted by the merge,
        * print There is an untracked file in the way; delete it, or add and commit it first.
        * and exit; perform this check before doing anything else.
        * */
        for (String file: CWD.list()) {
            File path = Utils.join(CWD, file);
            if (path.isFile()) {
                byte[] blank = {0};
                byte[] contents = Utils.readContents(path);
                byte[] otherContents = blank;
                byte[] splitContents = blank;
                byte[] mostRecentCommitContents = blank;
                if (otherBranchFiles.containsKey(file)) {
                    otherContents = otherBranchFiles.get(file);
                }
                if (splitPointFiles.containsKey(file)) {
                    splitContents = splitPointFiles.get(file);
                }
                if (mostRecentCommit.trackedFileContents.containsKey(file)) {
                    mostRecentCommitContents = mostRecentCommit.trackedFileContents.get(file);
                }
                //would be overwritten or deleted by merge = exists in some way in other
                if (!Arrays.equals(contents, mostRecentCommitContents) && (!Arrays.equals(contents, otherContents))) {
                    System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
                    return;
                }







            }
        }

        for (String file: allFileNames) {
            byte[] blank = {0};
            byte[] currentBranchContent = blank;
            byte[] splitPointContent = blank;
            byte[] otherBranchContent = blank;


            if (mostRecentCommit.trackedFileContents.containsKey(file)) {
                currentBranchContent = mostRecentCommit.trackedFileContents.get(file);
            }

            // checks if split point contains the file from CWD
            if (splitPointFiles.containsKey(file)) {
                splitPointContent = splitPointFiles.get(file);
            }
            //checks if other branch head commit contains file from CWD
            if (otherBranchFiles.containsKey(file)) {
                otherBranchContent = otherBranchCommit.trackedFileContents.get(file);
            }

            // any files that were not present at the split point
            // and are present only in the given branch should be
            // checked out and staged
            if (!splitPointFiles.containsKey(file) &&
                    otherBranchContent != blank && currentBranchContent == blank) {
                File path = Utils.join(CWD, file);
                Utils.writeContents(path, new String(otherBranchContent));
                stagingArea.put(file, otherBranchContent);
            }
            // if file modified in other branch, but not modified in
            // current branch, change cwd files to modified files from
            // other branch and then added to staging area

            // if file modified in current branch but not in other branch
            // then stays as they are
            else if (otherBranchContent != blank &&
                    otherBranchContent != splitPointContent &&
                    currentBranchContent == splitPointContent) {
                File path = Utils.join(CWD, file);
                Utils.writeContents(path, new String(otherBranchContent));
                stagingArea.put(file, otherBranchContent);

            // any files present at the split point, unmodified in the
            // current branch and absent in the given branch should be
            // removed and untracked
            } else if (splitPointContent != blank &&
                    Arrays.equals(currentBranchContent,splitPointContent) &&
                    otherBranchContent == blank) {
                    if (stagingArea.containsKey(file)) {
                        stagingArea.remove(file);
                    }
                    File path = Utils.join(CWD, file);
                    Utils.restrictedDelete(path);
            } else if ((!Arrays.equals(currentBranchContent, splitPointContent) &&
                    !Arrays.equals(otherBranchContent, splitPointContent) &&
                    !Arrays.equals(currentBranchContent, otherBranchContent))) {

                String newContents = "<<<<<<< HEAD\n";
                if (currentBranchContent != blank) {
                    newContents += new String(currentBranchContent);
                } else {
                    newContents += "\n";
                }
                newContents += "=======\n";

                if (otherBranchContent != blank) {
                    newContents += new String(otherBranchContent);
                }

                newContents += ">>>>>>>\n";
                File path = Utils.join(CWD, file);
                Utils.writeContents(path, newContents);
                hadConflict = true;
                stagingArea.put(file, Utils.readContents(path));
            }
        }
        commit("Merged " + branchName + " into " + this.currentBranch + ".");
        if (hadConflict) {
            System.out.println("Encountered a merge conflict.");
        }
    }

    // Merge commits differ from other commits: they record as parents both the head of the current branch
    // (called the first parent) and the head of the branch given on the command line to be merged in.




}
