import { Module } from '@nestjs/common';
import { ConfigModule, ConfigService } from '@nestjs/config';
import { TypeOrmModule } from '@nestjs/typeorm';
import { AppController } from './app.controller';
import { AppService } from './app.service';
import { HealthController } from './api/controller/health.controller';
import { OrderController } from './api/controller/order.controller';
import { CouponController } from './api/controller/coupon.controller';
import { OrderService } from './core/services/order.service';
import { CouponService } from './core/services/coupon.service';
import { ErpGateway } from './core/services/external/erp.gateway';
import { ClockGateway } from './core/services/external/clock.gateway';
import { TaxGateway } from './core/services/external/tax.gateway';
import { Order } from './core/entities/order.entity';
import { Coupon } from './core/entities/coupon.entity';

@Module({
  imports: [
    ConfigModule.forRoot({
      isGlobal: true,
    }),
    TypeOrmModule.forRootAsync({
      imports: [ConfigModule],
      inject: [ConfigService],
      useFactory: (configService: ConfigService) => {
        const host = configService.get<string>('POSTGRES_DB_HOST', 'localhost');
        const port = configService.get<number>('POSTGRES_DB_PORT', 5432);
        const database = configService.get<string>('POSTGRES_DB_NAME', 'app');
        const username = configService.get<string>('POSTGRES_DB_USER', 'app');
        const password = configService.get<string>(
          'POSTGRES_DB_PASSWORD',
          'app',
        );
        return {
          type: 'postgres' as const,
          host,
          port,
          database,
          username,
          password,
          entities: [Order, Coupon],
          synchronize: true,
          logging: configService.get<string>('NODE_ENV') !== 'production',
        };
      },
    }),
    TypeOrmModule.forFeature([Order, Coupon]),
  ],
  controllers: [
    AppController,
    HealthController,
    OrderController,
    CouponController,
  ],
  providers: [
    AppService,
    OrderService,
    CouponService,
    ErpGateway,
    ClockGateway,
    TaxGateway,
  ],
})
export class AppModule {}
