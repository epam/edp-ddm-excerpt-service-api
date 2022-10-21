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
{{- if .Values.keycloak.customHost }}
{{- printf "%s-%s" (include "keycloak.customUrlPrefix" .) .Values.keycloak.realms.officer -}}
{{- else }}
{{- printf "%s-%s" (include "keycloak.urlPrefix" .) .Values.keycloak.realms.officer -}}
{{- end }}
{{- end -}}

{{- define "issuer.citizen" -}}
{{- if .Values.keycloak.customHost }}
{{- printf "%s-%s" (include "keycloak.customUrlPrefix" .) .Values.keycloak.realms.citizen -}}
{{- else }}
{{- printf "%s-%s" (include "keycloak.urlPrefix" .) .Values.keycloak.realms.citizen -}}
{{- end }}
{{- end -}}

{{- define "issuer.admin" -}}
{{- printf "%s-%s" (include "keycloak.urlPrefix" .) .Values.keycloak.realms.admin -}}
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
