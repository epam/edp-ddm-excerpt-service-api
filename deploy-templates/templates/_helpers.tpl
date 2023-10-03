{{- define "keycloak.url" -}}
{{- printf "%s%s" "https://" .Values.keycloak.host }}
{{- end -}}

{{- define "keycloak.customUrl" -}}
{{- printf "%s%s" "https://" .Values.keycloak.customHost }}
{{- end -}}

{{- define "keycloak.urlPrefix" -}}
{{- printf "%s%s%s" (include "keycloak.url" .) "/auth/realms/" .Release.Namespace -}}
{{- end -}}

{{- define "keycloak.customUrlPrefix" -}}
{{- printf "%s%s%s" (include "keycloak.customUrl" .) "/auth/realms/" .Release.Namespace -}}
{{- end -}}

{{- define "issuer.officer" -}}
{{- printf "%s-%s" (include "keycloak.urlPrefix" .) .Values.keycloak.realms.officer -}}
{{- end -}}

{{- define "issuer.citizen" -}}
{{- printf "%s-%s" (include "keycloak.urlPrefix" .) .Values.keycloak.realms.citizen -}}
{{- end -}}

{{- define "issuer.admin" -}}
{{- printf "%s-%s" (include "keycloak.urlPrefix" .) .Values.keycloak.realms.admin -}}
{{- end -}}

{{- define "custom-issuer.officer" -}}
{{- printf "%s-%s" (include "keycloak.customUrlPrefix" .) .Values.keycloak.realms.officer -}}
{{- end -}}

{{- define "custom-issuer.citizen" -}}
{{- printf "%s-%s" (include "keycloak.customUrlPrefix" .) .Values.keycloak.realms.citizen -}}
{{- end -}}

{{- define "custom-issuer.admin" -}}
{{- printf "%s-%s" (include "keycloak.customUrlPrefix" .) .Values.keycloak.realms.admin -}}
{{- end -}}

{{- define "jwksUri.officer" -}}
{{- printf "%s-%s%s" (include "keycloak.urlPrefix" .) .Values.keycloak.realms.officer .Values.keycloak.certificatesEndpoint -}}
{{- end -}}

{{- define "jwksUri.citizen" -}}
{{- printf "%s-%s%s" (include "keycloak.urlPrefix" .) .Values.keycloak.realms.citizen .Values.keycloak.certificatesEndpoint -}}
{{- end -}}

{{- define "jwksUri.admin" -}}
{{- printf "%s-%s%s" (include "keycloak.urlPrefix" .) .Values.keycloak.realms.admin .Values.keycloak.certificatesEndpoint -}}
{{- end -}}

{{- define "imageRegistry" -}}
{{- if .Values.global.imageRegistry -}}
{{- printf "%s/" .Values.global.imageRegistry -}}
{{- else -}}
{{- end -}}
{{- end }}

{{- define "horizontalPodAutoscaler.apiVersion" }}
{{- if eq .Values.global.clusterVersion "4.9.0" }}
{{- printf "%s" "autoscaling/v2beta2" }}
{{- else }}
{{- printf "%s" "autoscaling/v2" }}
{{- end }}
{{- end }}

{{- define "excerptServiceApi.istioResources" -}}
{{- if .Values.global.registry.excerptServiceApi.istio.sidecar.resources.limits.cpu }}
sidecar.istio.io/proxyCPULimit: {{ .Values.global.registry.excerptServiceApi.istio.sidecar.resources.limits.cpu | quote }}
{{- else if and (not .Values.global.registry.excerptServiceApi.istio.sidecar.resources.limits.cpu) .Values.global.istio.sidecar.resources.limits.cpu }}
sidecar.istio.io/proxyCPULimit: {{ .Values.global.istio.sidecar.resources.limits.cpu | quote }}
{{- end }}
{{- if .Values.global.registry.excerptServiceApi.istio.sidecar.resources.limits.memory }}
sidecar.istio.io/proxyMemoryLimit: {{ .Values.global.registry.excerptServiceApi.istio.sidecar.resources.limits.memory | quote }}
{{- else if and (not .Values.global.registry.excerptServiceApi.istio.sidecar.resources.limits.memory) .Values.global.istio.sidecar.resources.limits.memory }}
sidecar.istio.io/proxyMemoryLimit: {{ .Values.global.istio.sidecar.resources.limits.memory | quote }}
{{- end }}
{{- if .Values.global.registry.excerptServiceApi.istio.sidecar.resources.requests.cpu }}
sidecar.istio.io/proxyCPU: {{ .Values.global.registry.excerptServiceApi.istio.sidecar.resources.requests.cpu | quote }}
{{- else if and (not .Values.global.registry.excerptServiceApi.istio.sidecar.resources.requests.cpu) .Values.global.istio.sidecar.resources.requests.cpu }}
sidecar.istio.io/proxyCPU: {{ .Values.global.istio.sidecar.resources.requests.cpu | quote }}
{{- end }}
{{- if .Values.global.registry.excerptServiceApi.istio.sidecar.resources.requests.memory }}
sidecar.istio.io/proxyMemory: {{ .Values.global.registry.excerptServiceApi.istio.sidecar.resources.requests.memory | quote }}
{{- else if and (not .Values.global.registry.excerptServiceApi.istio.sidecar.resources.requests.memory) .Values.global.istio.sidecar.resources.requests.memory }}
sidecar.istio.io/proxyMemory: {{ .Values.global.istio.sidecar.resources.requests.memory | quote }}
{{- end }}
{{- end -}}
