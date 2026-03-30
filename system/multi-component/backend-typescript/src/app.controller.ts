import { Controller, Get, Param, Res } from '@nestjs/common';
import type { Response } from 'express';
import { AppService } from './app.service';

@Controller()
export class AppController {
  constructor(private readonly appService: AppService) {}

  @Get()
  getHomePage(@Res() res: Response): void {
    res.send(this.appService.getHomePageHtml());
  }

  @Get('api/echo')
  getEcho(): string {
    return this.appService.getEcho();
  }

  @Get('api/todos/:id')
  async getTodo(@Param('id') id: string): Promise<any> {
    return await this.appService.getTodo(Number(id));
  }

  @Get('todos')
  getTodosPage(@Res() res: Response): void {
    res.send(this.appService.getTodosPageHtml());
  }
}
