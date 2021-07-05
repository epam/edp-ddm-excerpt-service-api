<html>
<head>
  <meta charset="utf-8" />
  <style>
    * { font-family: Roboto; }
  </style>
</head>
<body>

<h1 align="center">Витяг з реєстру [=registryName]</h1>
<table align="center">
  <tr>
    <th>Ідентифікатор</th>
    <th>Ім'я чиновника</th>
    <th>Операція</th>
    <th>Час операції</th>
    <th>Процес</th>
  </tr>
    [#list requests as request]
      <tr>
        <td>[=request.id]</td>
        <td>[=request.officerName]</td>
        <td>[=request.operation]</td>
        <td>[=request.time]</td>
        <td>[=request.businessProcess]</td>
      </tr>
    [/#list]
</table>
</body>
</html>