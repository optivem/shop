import { useMemo, useState } from 'react';
import {
  useReactTable,
  getCoreRowModel,
  getSortedRowModel,
  createColumnHelper,
  flexRender,
  type SortingState,
} from '@tanstack/react-table';
import { SortableHeaderCell, TableDataState } from '../../components';
import { UNLIMITED_USAGE_LIMIT, type BrowseCouponsItemResponse } from '../../types/api.types';

const columnHelper = createColumnHelper<BrowseCouponsItemResponse>();

interface CouponTableProps {
  coupons: BrowseCouponsItemResponse[];
  isLoading: boolean;
  error: string | null;
  getCouponStatus: (coupon: BrowseCouponsItemResponse) => string;
  onRefresh: () => void;
}

export function CouponTable({ coupons, isLoading, error, getCouponStatus, onRefresh }: Readonly<CouponTableProps>) {
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
          return value === null || value === UNLIMITED_USAGE_LIMIT ? 'Unlimited' : value;
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

  // eslint-disable-next-line react-hooks/incompatible-library -- TanStack Table's useReactTable is inherently incompatible with React Compiler memoization; no compiler is used here and there is no compatible API to switch to
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
                    <SortableHeaderCell key={header.id} header={header} />
                  ))}
                </tr>
              ))}
            </thead>
            <tbody>
              {isLoading || error || table.getRowModel().rows.length === 0 ? (
                <TableDataState
                  isLoading={isLoading}
                  error={error}
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
