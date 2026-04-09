package gitlet;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author areeya
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ... 
     */
    public static void main(String[] args) {

        String repoFilePath = Repository.GITLET_DIR + "/repo.txt";
        File repositoryFile = new File(repoFilePath);
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            return;
        }
            // TODO: what if args is empty?
            String firstArg = args[0];
            List<String> validArgs = Arrays.asList("init", "add", "commit", "log", "restore", "rm", "global-log",
                    "find", "status", "branch", "switch", "rm-branch", "reset", "merge");
            if (!validArgs.contains(firstArg)) {
                System.out.println("No command with that name exists.");
            } else {
                switch (firstArg) {
                    case "init":
                        if (repositoryFile.exists()) {
                            System.out.println("A Gitlet version-control system already exists in the current directory.");
                            break;
                        }
                        Repository r = new Repository();
                        if (!Repository.GITLET_DIR.exists()) {
                            Repository.GITLET_DIR.mkdir();
                        }
                        Utils.writeObject(repositoryFile, r);

                        break;
                    case "add":
                        if (!Repository.GITLET_DIR.exists()) {
                            System.out.println("Not in an initialized Gitlet directory.");
                            break;
                        }
                        Repository repo = Utils.readObject(repositoryFile, Repository.class);
                        //System.out.println("add has a mostRecentCommit " + (repo.mostRecentCommit != null));
                        repo.add(args[1]);
                        Utils.writeObject(repositoryFile, repo);
                        break;
                    case "commit":
                        if (!Repository.GITLET_DIR.exists()) {
                            System.out.println("Not in an initialized Gitlet directory.");
                            break;
                        }

                        repo = Utils.readObject(repositoryFile, Repository.class);
                        repo.commit((args)[1]);
                        //System.out.println("files in commit: " + repo.mostRecentCommit.trackedFileContents.keySet());
                        Utils.writeObject(repositoryFile, repo);
                        break;

                    case "log":
                        if (!Repository.GITLET_DIR.exists()) {
                            System.out.println("Not in an initialized Gitlet directory.");
                            break;

                        }

                        repo = Utils.readObject(repositoryFile, Repository.class);
                        repo.printLog();
                        break;
                    case "restore":
                        if (!Repository.GITLET_DIR.exists()) {
                            System.out.println("Not in an initialized Gitlet directory.");
                            break;
                        }

                        repo = Utils.readObject(repositoryFile, Repository.class);

                        if (args.length == 4) {
                            if (!args[2].equals("--")) {
                                System.out.println("Incorrect operands.");
                                break;
                            }
                            String commitIdArg = args[1];
                            String fileToRestoreArg = args[3];
                            repo.restore(commitIdArg, fileToRestoreArg);
                        } else if (args.length == 3) {
                            String fileToRestoreArg = args[2];
                            repo.restore(fileToRestoreArg);
                        } else {
                            System.out.println("Invalid arguments for restore");
                        }

                        Utils.writeObject(repositoryFile, repo);
                        break;
                    case "rm":
                        if (!Repository.GITLET_DIR.exists()) {
                            System.out.println("Not in an initialized Gitlet directory.");
                            break;
                        }

                        //System.out.println("remove??? i hardly know her move!!!");
                        repo = Utils.readObject(repositoryFile, Repository.class);
                        if (args.length == 2) {
                            repo.rm(args[1]);
                            Utils.writeObject(repositoryFile, repo);
                        } else {
                            System.out.println("Invalid arguments for remove.");
                        }
                        break;
                    //NEW
                    case "global-log":
                        if (!Repository.GITLET_DIR.exists()) {
                            System.out.println("Not in an initialized Gitlet directory.");
                            break;
                        }

                        repo = Utils.readObject(repositoryFile, Repository.class);
                        repo.printGlobalLog();
                        break;
                    case "find":
                        if (!Repository.GITLET_DIR.exists()) {
                            System.out.println("Not in an initialized Gitlet directory.");
                            break;
                        }

                        repo = Utils.readObject(repositoryFile, Repository.class);
                        if (args[0].equals("find")) {
                            String commitMessage = args[1];
                            repo.find(commitMessage);
                            Utils.writeObject(repositoryFile, repo);
                        }
                        break;
                    case "status":
                        if (!Repository.GITLET_DIR.exists()) {
                            System.out.println("Not in an initialized Gitlet directory.");
                            break;
                        }

                        repo = Utils.readObject(repositoryFile, Repository.class);
                        repo.printStatus();
                        break;
                    case "branch":
                        if (!Repository.GITLET_DIR.exists()) {
                            System.out.println("Not in an initialized Gitlet directory.");
                            break;
                        }

                        repo = Utils.readObject(repositoryFile, Repository.class);
                        if (args.length == 2) {
                            repo.branch(args[1]);
                            Utils.writeObject(repositoryFile, repo);
                        } else {
                            System.out.println("Invalid");
                        }
                        break;
                    case "switch":
                        if (!Repository.GITLET_DIR.exists()) {
                            System.out.println("Not in an initialized Gitlet directory.");
                            break;
                        }

                        repo = Utils.readObject(repositoryFile, Repository.class);
                        if (args.length == 2) {
                            repo.switchBranch(args[1]);
                        } else {
                            System.out.println("Invalid");
                            break;
                        }
                        Utils.writeObject(repositoryFile, repo);
                        break;
                    case "rm-branch":
                        if (!Repository.GITLET_DIR.exists()) {
                            System.out.println("Not in an initialized Gitlet directory.");
                            break;
                        }

                        repo = Utils.readObject(repositoryFile, Repository.class);
                        if (args.length == 2) {
                            String branchName = args[1];
                            repo.rmBranch(branchName);
                            Utils.writeObject(repositoryFile, repo);
                        } else {
                            System.out.println("remove");
                        }
                        break;
                    case "reset":
                        if (!Repository.GITLET_DIR.exists()) {
                            System.out.println("Not in an initialized Gitlet directory.");
                            break;
                        }

                        if (args.length != 2) {
                            System.out.println("commit id");
                            break;
                        }
                        String commitId = args[1];
                        repo = Utils.readObject(repositoryFile, Repository.class);
                        repo.reset(commitId);
                        Utils.writeObject(repositoryFile, repo);
                        break;
                    case "merge":
                        if (!Repository.GITLET_DIR.exists()) {
                            System.out.println("Not in an initialized Gitlet directory.");
                            break;
                        }
                        repo = Utils.readObject(repositoryFile, Repository.class);
                        repo.merge(args[1]);

                        Utils.writeObject(repositoryFile, repo);
                        break;
                    case "":
                        if (!Repository.GITLET_DIR.exists()) {
                            System.out.println("Not in an initialized Gitlet directory.");
                            break;
                        }

                        System.out.println("hm? can you repeat that?");
                        break;
                }
            }
        }
    }


