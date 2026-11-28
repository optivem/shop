import { useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import {
  useReactTable,
  getCoreRowModel,
  getSortedRowModel,
  createColumnHelper,
  flexRender,
  type SortingState,
} from '@tanstack/react-table';
import { RefreshCw } from 'lucide-react';
import { formatDateTime, formatPercent } from '../../i18n/format';
import { SortableHeaderCell, TableDataState } from '../../components';
import { UNLIMITED_USAGE_LIMIT, type BrowseCouponsItemResponse } from '../../types/api.types';

const columnHelper = createColumnHelper<BrowseCouponsItemResponse>();

/** Colour of the status badge; `getCouponStatus` keeps returning the English status, shown through `statusLabel`. */
function statusClass(status: string): string {
  switch (status) {
    case 'Active':
      return 'coupon-status-active';
    case 'Not Yet Valid':
      return 'coupon-status-scheduled';
    default:
      return 'coupon-status-inactive';
  }
}

interface CouponTableProps {
  coupons: BrowseCouponsItemResponse[];
  isLoading: boolean;
  error: string | null;
  getCouponStatus: (coupon: BrowseCouponsItemResponse) => string;
  onRefresh: () => void;
}

export function CouponTable({ coupons, isLoading, error, getCouponStatus, onRefresh }: Readonly<CouponTableProps>) {
  const { t } = useTranslation('account');
  const [sorting, setSorting] = useState<SortingState>([]);

  const statusLabel = (status: string): string => {
    switch (status) {
      case 'Active':
        return t('couponTable.status.active');
      case 'Not Yet Valid':
        return t('couponTable.status.notYetValid');
      case 'Expired':
        return t('couponTable.status.expired');
      case 'Limit Reached':
        return t('couponTable.status.limitReached');
      default:
        return status;
    }
  };

  const columns = useMemo(
    () => [
      columnHelper.accessor('code', {
        header: t('couponTable.columns.code'),
        cell: (info) => info.getValue(),
      }),
      columnHelper.accessor('discountRate', {
        header: t('couponTable.columns.discountRate'),
        cell: (info) => formatPercent(info.getValue()),
      }),
      columnHelper.accessor('validFrom', {
        header: t('couponTable.columns.validFrom'),
        cell: (info) => {
          const value = info.getValue();
          return value ? formatDateTime(value) : t('couponTable.immediate');
        },
        sortingFn: 'datetime',
      }),
      columnHelper.accessor('validTo', {
        header: t('couponTable.columns.validTo'),
        cell: (info) => {
          const value = info.getValue();
          return value ? formatDateTime(value) : t('couponTable.never');
        },
        sortingFn: 'datetime',
      }),
      columnHelper.accessor('usageLimit', {
        header: t('couponTable.columns.usageLimit'),
        cell: (info) => {
          const value = info.getValue();
          return value === null || value === UNLIMITED_USAGE_LIMIT ? t('couponTable.unlimited') : value;
        },
      }),
      columnHelper.accessor('usedCount', {
        header: t('couponTable.columns.usedCount'),
        cell: (info) => info.getValue(),
      }),
      columnHelper.display({
        id: 'status',
        header: t('couponTable.columns.status'),
        cell: (info) => {
          const status = getCouponStatus(info.row.original);
          return <span className={`badge coupon-status ${statusClass(status)}`}>{statusLabel(status)}</span>;
        },
      }),
    ],
    // eslint-disable-next-line react-hooks/exhaustive-deps -- statusLabel only depends on t
    [getCouponStatus, t]
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
    <section className="market-panel coupon-panel" aria-labelledby="coupon-table-heading">
      <div className="coupon-panel-header">
        <div>
          <h2 id="coupon-table-heading" className="coupon-panel-title">{t('couponTable.heading')}</h2>
          {!isLoading && !error && (
            <p className="coupon-panel-subtitle">
              {t('couponTable.count', { count: coupons.length })}
            </p>
          )}
        </div>
        <button
          type="button"
          className="btn btn-outline-secondary coupon-refresh"
          onClick={onRefresh}
          disabled={isLoading}
          aria-label={t('couponTable.refreshAria')}
        >
          <RefreshCw size={16} aria-hidden="true" />
          {t('couponTable.refresh')}
        </button>
      </div>
      <div className="table-responsive coupon-table-scroll">
        <table className="table table-hover align-middle mb-0 coupon-table" aria-label={t('couponTable.tableAria')}>
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
                loadingMessage={t('couponTable.loading')}
                emptyMessage={t('couponTable.empty')}
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
    </section>
  );
}
