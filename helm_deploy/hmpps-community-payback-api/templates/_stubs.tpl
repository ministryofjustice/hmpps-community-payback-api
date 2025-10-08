{{- define "keyForPath" -}}
{{- $rel := . -}}
{{- $hash := (sha256sum $rel) | trunc 12 -}}
{{- $safe := regexReplaceAll "[^A-Za-z0-9._-]" (regexReplaceAll "/" "_" $rel) "_" -}}
{{- printf "%s__%s" $hash $safe -}}
{{- end -}}
