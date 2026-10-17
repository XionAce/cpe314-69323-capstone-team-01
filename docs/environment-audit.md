# Environment audit — 17 October 2026, Asia/Manila

| Item | Observed result |
|---|---|
| OS architecture | X64 |
| Git | git version 2.54.0.windows.1; available on PATH |
| Antigravity | Installation directory found under the user's Local Programs directory; launch/login unverified |
| java / javac | Not found on PATH |
| Maven | Not found on PATH |
| Eclipse | Not found on PATH or in the common locations checked |
| JDK installations | No JDK found in the common Program Files locations checked |
| WinGet | Command located; installation operations not attempted |
| Workspace | ChatGPT project mirror; synced sources/ treated as read-only |

Missing from PATH does not prove a program is absent everywhere. The audit checked common installation directories and matching uninstall entries, not every disk directory. No tools were installed or system environment variables changed.

## Setup attempt

A local Git repository was initialized on main in the preparation folder. Drafts remain uncommitted; no remote was created and no account identity was changed.

After network permission was granted, downloading the official portable JDK was attempted with PowerShell and curl. Both failed during TLS setup. curl reported Schannel SEC_E_NO_CREDENTIALS (0x8009030E); PowerShell reported authentication failure. No successful JDK download or extraction occurred. Use the official download page in a browser for manual installation, then rerun the version checks. This is a download-channel failure, not evidence that the vendor endpoint or JDK is invalid.

Next checks after installation: java -version, javac -version, then Maven Wrapper's -version after bootstrap. No Java compile, JUnit, database integration, or GUI launch has been performed.
