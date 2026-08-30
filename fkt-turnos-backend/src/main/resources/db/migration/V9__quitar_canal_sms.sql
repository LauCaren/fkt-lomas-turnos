-- Se saca el canal SMS: a partir de ahora los recordatorios y el mensaje
-- puntual solo se pueden mandar por email o WhatsApp. Si algún registro
-- quedó con SMS guardado, se pasa a EMAIL (el default) antes de que el
-- enum de Java deje de reconocer ese valor.
UPDATE usuarios SET canal_notificacion_preferido = 'EMAIL' WHERE canal_notificacion_preferido = 'SMS';
UPDATE recordatorios SET canal = 'EMAIL' WHERE canal = 'SMS';
