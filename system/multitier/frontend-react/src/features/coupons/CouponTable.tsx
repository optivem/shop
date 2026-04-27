import { useMemo, useState } from 'react';
import {
  useReactTable,
  getCoreRowModel,
  getSortedRowModel,
  createColumnHelper,
  flexRender,
  type SortingState,
} from '@tanstack/react-table';
import { TableDataState } from '../../components';
import type { BrowseCouponsItemResponse } from '../../types/api.types';

const columnHelper = createColumnHelper<BrowseCouponsItemResponse>();

interface CouponTableProps {
  coupons: BrowseCouponsItemResponse[];
  isLoading: boolean;
  getCouponStatus: (coupon: BrowseCouponsItemResponse) => string;
  onRefresh: () => void;
}

export function CouponTable({ coupons, isLoading, getCouponStatus, onRefresh }: Readonly<CouponTableProps>) {
  const [sorting, setSorting] = useState<SortingState>([]);

  const columns = useMemo(
    () => [
      columnHelper.accessor('code', {
        header: 'Code',
        cell: (info) => info.getValue(),
      }),
      columnHelper.accessor('discountRate', {
        header: 'Discount Rate',
        cell: (info) => `${(info.getValue() * 100).toFixed(2)}%`,
      }),
      columnHelper.accessor('validFrom', {
        header: 'Valid From',
        cell: (info) => {
          const value = info.getValue();
          return value ? new Date(value).toLocaleString('en-US', { timeZone: 'UTC' }) : 'Immediate';
        },
        sortingFn: 'datetime',
      }),
      columnHelper.accessor('validTo', {
        header: 'Valid To',
        cell: (info) => {
          const value = info.getValue();
          return value ? new Date(value).toLocaleString('en-US', { timeZone: 'UTC' }) : 'Never';
        },
        sortingFn: 'datetime',
      }),
      columnHelper.accessor('usageLimit', {
        header: 'Usage Limit',
        cell: (info) => {
          const value = info.getValue();
          return value === null || value === 2147483647 ? 'Unlimited' : value;
        },
      }),
      columnHelper.accessor('usedCount', {
        header: 'Used Count',
        cell: (info) => info.getValue(),
      }),
      columnHelper.display({
        id: 'status',
        header: 'Status',
        cell: (info) => getCouponStatus(info.row.original),
      }),
    ],
    [getCouponStatus]
  );

  const table = useReactTable({
    data: coupons,
    columns,
    state: {
      sorting,
    },
    onSortingChange: setSorting,
    getCoreRowModel: getCoreRowModel(),
    getSortedRowModel: getSortedRowModel(),
  });

  return (
    <div className="card shadow">
      <div className="card-header bg-primary text-white d-flex justify-content-between align-items-center">
        <h4 className="mb-0">Existing Coupons</h4>
        <button
          className="btn btn-light btn-sm"
          onClick={onRefresh}
          disabled={isLoading}
          aria-label="Refresh Coupon List"
        >
          Refresh
        </button>
      </div>
      <div className="card-body">
        <div className="table-responsive">
          <table className="table table-striped table-hover" aria-label="Coupons Table">
            <thead>
              {table.getHeaderGroups().map((headerGroup) => (
                <tr key={headerGroup.id}>
                  {headerGroup.headers.map((header) => (
                    <th
                      key={header.id}
                      onClick={header.column.getToggleSortingHandler()}
                      style={{ cursor: header.column.getCanSort() ? 'pointer' : 'default' }}
                    >
                      <div className="d-flex align-items-center">
                        {flexRender(
                          header.column.columnDef.header,
                          header.getContext()
                        )}
                        {header.column.getIsSorted() && (
                          <span className="ms-1">
                            {header.column.getIsSorted() === 'asc' ? '↑' : '↓'}
                          </span>
                        )}
                      </div>
                    </th>
                  ))}
                </tr>
              ))}
            </thead>
            <tbody>
              {isLoading || table.getRowModel().rows.length === 0 ? (
                <TableDataState
                  isLoading={isLoading}
                  isEmpty={table.getRowModel().rows.length === 0}
                  colSpan={columns.length}
                  loadingMessage="Loading coupons..."
                  emptyMessage="No coupons found"
                  onRetry={onRefresh}
                />
              ) : (
                table.getRowModel().rows.map((row) => (
                  <tr key={row.id}>
                    {row.getVisibleCells().map((cell) => (
                      <td key={cell.id}>
                        {flexRender(cell.column.columnDef.cell, cell.getContext())}
                      </td>
                    ))}
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
