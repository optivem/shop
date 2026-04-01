import { Module } from '@nestjs/common';
import { ConfigModule, ConfigService } from '@nestjs/config';
import { TypeOrmModule } from '@nestjs/typeorm';
import { AppController } from './app.controller';
import { AppService } from './app.service';
import { HealthController } from './api/controller/health.controller';
import { OrderController } from './api/controller/order.controller';
import { OrderService } from './core/services/order.service';
import { ErpGateway } from './core/services/external/erp.gateway';
import { ClockGateway } from './core/services/external/clock.gateway';
import { Order } from './core/entities/order.entity';

@Module({
  imports: [
    ConfigModule.forRoot({
      isGlobal: true,
    }),
    TypeOrmModule.forRootAsync({
      imports: [ConfigModule],
      inject: [ConfigService],
      useFactory: (configService: ConfigService) => ({
        type: 'postgres' as const,
        url: configService.get<string>('POSTGRES_URL', 'postgresql://starter:starter@localhost:5432/starter'),
        entities: [Order],
        synchronize: true,
        logging: configService.get<string>('NODE_ENV') !== 'production',
      }),
    }),
    TypeOrmModule.forFeature([Order]),
  ],
  controllers: [AppController, HealthController, OrderController],
  providers: [AppService, OrderService, ErpGateway, ClockGateway],
})
export class AppModule {}
