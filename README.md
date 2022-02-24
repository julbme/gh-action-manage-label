[![Build](https://github.com/julbme/gh-action-manage-label/actions/workflows/maven-build.yml/badge.svg)](https://github.com/julbme/gh-action-manage-label/actions/workflows/maven-build.yml)
[![Lint Commit Messages](https://github.com/julbme/gh-action-manage-label/actions/workflows/commitlint.yml/badge.svg)](https://github.com/julbme/gh-action-manage-label/actions/workflows/commitlint.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=julbme_gh-action-manage-label&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=julbme_gh-action-manage-label)
![GitHub release (latest SemVer)](https://img.shields.io/github/v/release/julbme/gh-action-manage-label)

# GitHub Action to manage labels

The GitHub Action for managing labels of the GitHub repository from a provided file.
It is particularly helpful when you want to setup the same labels for all your repositories.

## Usage

### Example Workflow file

- Synchronize labels from a YAML file:

```yaml
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout
        uses: actions/checkout@v2

      - name: Synchronize labels
        uses: julbme/gh-action-manage-label@v1
        with:
          from: .github/config/labels.yml
          skip_delete: false
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

- Synchronize labels from multiple YAML files:

```yaml
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout
        uses: actions/checkout@v2

      - name: Synchronize labels
        uses: julbme/gh-action-manage-label@v1
        with:
          from: |
            .github/config/labels.yml
            .github/config/other-labels.yml
            https://some-host.com/labels.ymls
          skip_delete: false
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

### Inputs

| Name          | Type    | Default                     | Description                                                                                                                                              |
| ------------- | ------- | --------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `from`        | string  | `.github/config/labels.yml` | Path of the file containing the labels. Can be `.yml`, `.yaml` or `.json`. A HTTP/HTTPS URL can be provided if the file is stored in another repository. |
| `skip_delete` | boolean | `false`                     | If `true`, the process will not delete existing labels that are not defined in the file.                                                                 |

> **Note** : the `from` field also accepts a multi-line parameter in order to accept multiple source files. If there is an overlap on the labels between the files, the last one will take over the others.

### Outputs

No output.

## Contributing

This project is totally open source and contributors are welcome.