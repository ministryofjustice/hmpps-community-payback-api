{{/* vim: set filetype=mustache: */}}
{{/*
Environment variables for scheduled database tasks
*/}}
{{- define "dbScheduledTasks.envs" -}}
{{- if or .dbScheduledTasks.namespace_secrets .dbScheduledTasks.env -}}
env:
{{- range $secret, $envs := .dbScheduledTasks.namespace_secrets }}
  {{- range $key, $val := $envs }}
  - name: {{ $key }}
    valueFrom:
      secretKeyRef:
        key: {{ trimSuffix "?" $val }}
        name: {{ $secret }}{{ if hasSuffix "?" $val }}
        optional: true{{ end }}  {{- end }}
{{- end }}
{{- range $key, $val := .dbScheduledTasks.env }}
  - name: {{ $key }}
    value: "{{ $val }}"
{{- end }}
{{- end -}}
{{- end -}}
