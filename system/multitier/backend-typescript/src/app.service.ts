import { Injectable } from '@nestjs/common';

@Injectable()
export class AppService {
  getHello(): string {
    return 'Hello World!';
  }

  getHomePageHtml(): string {
    return `
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Starter</title>
</head>
<body>
    <h1>Hello World!</h1>
    <p>Welcome to the Starter - TypeScript</p>
</body>
</html>
    `;
  }
}
