# Local Environment Notes

Captured on 2026-03-18 in timezone Asia/Calcutta.

## Detected tools

- Java: available (`java version "25.0.1"`)
- Node.js: available (`v22.19.0`)
- Docker: available (`28.3.3`)
- Git: available (`2.47.0.windows.2`)
- kubectl: available (`v1.32.2`)

## Detected blockers

- `mvn` is not on `PATH`
- `python` is not on `PATH`
- `py --version` fails because the configured Python executable is not accessible
- `minikube` is not on `PATH`

## Recommended fixes

1. Install Maven 3.9+ and add it to `PATH`, or generate Maven wrappers once Maven is available.
2. Repair or reinstall Python 3.10+ so both `python --version` and `py --version` work.
3. Install Minikube if Week 5 local Kubernetes work will happen on this machine.
4. Re-run tool verification before attempting CI/CD or Kubernetes tasks.
