# Kubescope - Kubernetes Cost & Observability Dashboard

## 1. Overview
KubeScope is an open-source, self-hosted web application that connects to a Kubernetes cluster, collects real-time resource usage metrics (CPU, memory, pod counts, node health), maps them to AWS pricing models, and presents a live cost breakdown dashboard with configurable alerts. It is designed for DevOps engineers and platform teams who need cluster-level cost visibility without paying for enterprise observability tools.

## 2. Goals

Give engineers real-time visibility into what their Kubernetes workloads cost on AWS.
Provide namespace-level and deployment-level cost breakdowns.
Send alerts when costs or resource usage exceed defined thresholds.
Be fully deployable via Docker Compose (local) and Helm chart (production on EKS).
Be open source and well-documented so the community can contribute.