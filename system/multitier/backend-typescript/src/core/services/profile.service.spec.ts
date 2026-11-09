import type { Repository } from 'typeorm';
import { CustomerProfile } from '../entities/customer-profile.entity';
import { UpdateProfileRequest } from '../dtos/update-profile-request.dto';
import { ProfileService } from './profile.service';
import type { CurrentUser } from '../../auth/current-user';

const CUSTOMER: CurrentUser = {
  subject: 'sub-customer',
  username: 'customer1',
  admin: false,
  email: 'customer1@example.com',
  firstName: 'Token',
  lastName: 'Name',
};

const SAVED = {
  firstName: 'Ana',
  lastName: 'Novak',
  street: 'Main St 1',
  postalCode: '10115',
  city: 'Berlin',
  country: 'DE',
};

describe('ProfileService', () => {
  let repository: jest.Mocked<
    Pick<Repository<CustomerProfile>, 'findOne' | 'upsert'>
  >;
  let service: ProfileService;

  beforeEach(() => {
    repository = {
      findOne: jest.fn().mockResolvedValue(null),
      upsert: jest.fn().mockResolvedValue(undefined),
    };
    service = new ProfileService(
      repository as unknown as Repository<CustomerProfile>,
    );
  });

  it("looks the profile up by the caller's token subject", async () => {
    await service.viewProfile(CUSTOMER);
    expect(repository.findOne).toHaveBeenCalledWith({
      where: { owner: 'sub-customer' },
    });
  });

  it('defaults the name from the token and leaves the address empty when nothing is saved', async () => {
    await expect(service.viewProfile(CUSTOMER)).resolves.toEqual({
      firstName: 'Token',
      lastName: 'Name',
      email: 'customer1@example.com',
      street: null,
      postalCode: null,
      city: null,
      country: null,
    });
  });

  it('shows the saved profile with the email from the token', async () => {
    repository.findOne.mockResolvedValue(
      Object.assign(new CustomerProfile(), { owner: 'sub-customer', ...SAVED }),
    );
    await expect(service.viewProfile(CUSTOMER)).resolves.toEqual({
      ...SAVED,
      email: 'customer1@example.com',
    });
  });

  it("upserts the caller's profile by owner and returns it", async () => {
    const request = Object.assign(new UpdateProfileRequest(), SAVED);

    const response = await service.updateProfile(request, CUSTOMER);

    expect(repository.upsert).toHaveBeenCalledWith(
      expect.objectContaining({
        owner: 'sub-customer',
        ...SAVED,
        updatedAt: expect.any(Date) as Date,
      }),
      expect.objectContaining({ conflictPaths: ['owner'] }),
    );
    expect(response).toEqual({ ...SAVED, email: 'customer1@example.com' });
  });
});
