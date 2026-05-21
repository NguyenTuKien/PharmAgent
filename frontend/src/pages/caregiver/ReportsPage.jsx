import React, { useState, useEffect } from 'react'
import {
  TrendingUp,
  TrendingDown,
  AlertTriangle,
  CheckCircle2,
  Clock,
  User,
  Phone,
  RefreshCw,
  Calendar,
  AlertCircle,
  Bell,
  Heart,
  ShieldAlert,
  ChevronRight,
  Sparkles,
  Info,
  Pill,
} from 'lucide-react'
import {
  Area,
  AreaChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import { apiClient, getApiErrorMessage } from '../../lib/apiClient.js'
import { notify } from '../../lib/toast.js'

export function ReportsPage() {
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [refreshing, setRefreshing] = useState(false)

  const fetchStats = async (showRefreshIndicator = false) => {
    if (showRefreshIndicator) {
      setRefreshing(true)
    } else {
      setLoading(true)
    }
    try {
      const response = await apiClient.get('/caregiver/stats/overview')
      setData(response.data)
    } catch (error) {
      console.error('Error fetching caregiver statistics:', error)
      notify.error(getApiErrorMessage(error) || 'Không thể tải dữ liệu thống kê')
    } finally {
      setLoading(false)
      setRefreshing(false)
    }
  }

  useEffect(() => {
    fetchStats()
  }, [])

  // Format date key (YYYY-MM-DD) to Day name / Short date (dd/MM)
  const formatChartData = (trendData) => {
    if (!trendData || trendData.length === 0) return []
    return trendData.map((item) => {
      const dateParts = item.date.split('-')
      const formattedDate = dateParts.length === 3 ? `${dateParts[2]}/${dateParts[1]}` : item.date
      return {
        ...item,
        dateFormatted: formattedDate,
        'Tỉ lệ': item.adherencePercent,
      }
    });
  }

  // Get status color & label
  const getStatusBadge = (status) => {
    switch (status) {
      case 'TAKEN':
        return {
          bg: 'bg-emerald-50 text-emerald-700 border-emerald-200',
          label: 'Đã uống',
          icon: <CheckCircle2 className="w-3.5 h-3.5" />,
        }
      case 'MISSED':
        return {
          bg: 'bg-rose-50 text-rose-700 border-rose-200',
          label: 'Bỏ lỡ',
          icon: <AlertCircle className="w-3.5 h-3.5" />,
        }
      case 'OVERDUE':
        return {
          bg: 'bg-amber-50 text-amber-700 border-amber-200',
          label: 'Trễ giờ',
          icon: <Clock className="w-3.5 h-3.5 text-amber-500 animate-pulse" />,
        }
      case 'SKIPPED':
        return {
          bg: 'bg-slate-100 text-slate-600 border-slate-200',
          label: 'Bỏ qua',
          icon: <Info className="w-3.5 h-3.5" />,
        }
      case 'PENDING':
      default:
        return {
          bg: 'bg-sky-50 text-sky-700 border-sky-200',
          label: 'Sắp tới',
          icon: <Calendar className="w-3.5 h-3.5" />,
        }
    }
  }

  if (loading) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[400px] gap-4">
        <RefreshCw className="w-8 h-8 text-emerald-600 animate-spin" />
        <p className="text-sm font-semibold text-slate-500">Đang phân tích dữ liệu thống kê...</p>
      </div>
    )
  }

  const kpi = data?.kpi || {
    overallAdherence: 100,
    totalDosesToday: 0,
    takenDosesToday: 0,
    missedDosesToday: 0,
    attentionCount: 0,
  }

  const chartData = formatChartData(data?.weeklyAdherence)
  const attentionList = data?.attentionRelatives || []
  const timeline = data?.todayTimeline || []
  const warnings = data?.recentWarnings || []

  // Determine trend status
  const getTrendStatus = () => {
    if (chartData.length < 2) return { text: 'Chưa đủ dữ liệu', icon: null, color: 'text-slate-500' }
    const lastDay = chartData[chartData.length - 1].adherencePercent
    const prevDay = chartData[chartData.length - 2].adherencePercent
    if (lastDay > prevDay) {
      return {
        text: `Tăng ${Math.round(lastDay - prevDay)}% so với hôm qua`,
        icon: <TrendingUp className="w-4 h-4" />,
        color: 'text-emerald-600 bg-emerald-50 border-emerald-100',
      }
    } else if (lastDay < prevDay) {
      return {
        text: `Giảm ${Math.round(prevDay - lastDay)}% so với hôm qua`,
        icon: <TrendingDown className="w-4 h-4" />,
        color: 'text-rose-600 bg-rose-50 border-rose-100',
      }
    }
    return {
      text: 'Bằng tỉ lệ hôm qua',
      icon: null,
      color: 'text-slate-500 bg-slate-50 border-slate-100',
    }
  }

  const trend = getTrendStatus()

  return (
    <div className="max-w-7xl mx-auto px-4 py-6 space-y-8 select-none">
      
      {/* Top Welcome and Title */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <span className="text-xs font-black uppercase tracking-wider text-emerald-700 bg-emerald-50 border border-emerald-100 px-2.5 py-1 rounded-full">
            Caregiver Dashboard
          </span>
          <h1 className="mt-2 text-2xl font-black text-slate-900 tracking-tight flex items-center gap-2">
            Thống kê & Giám sát sức khỏe
            <Sparkles className="w-5 h-5 text-amber-500 fill-amber-500" />
          </h1>
          <p className="mt-1 text-slate-500 text-sm font-semibold leading-relaxed">
            Xem toàn bộ tình hình tuân thủ uống thuốc và mức độ an toàn của tất cả người thân trong gia đình.
          </p>
        </div>
        <button
          onClick={() => fetchStats(true)}
          disabled={refreshing}
          className="self-start sm:self-center inline-flex items-center gap-2 px-4 py-2 border border-slate-200 rounded-xl bg-white hover:bg-slate-50 text-slate-700 text-sm font-bold shadow-sm shadow-slate-100 active:scale-95 transition-all disabled:opacity-60"
        >
          <RefreshCw className={`w-4 h-4 ${refreshing ? 'animate-spin' : ''}`} />
          Cập nhật dữ liệu
        </button>
      </div>

      {/* 1. KPIs Section */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
        {/* KPI: Adherence */}
        <div
          className="bg-white rounded-2xl border border-slate-150 p-5 shadow-sm shadow-slate-200/50 flex flex-col justify-between min-h-[140px] relative overflow-hidden transition-all duration-300 hover:-translate-y-1"
        >
          <div className="absolute top-0 right-0 w-24 h-24 bg-gradient-to-br from-emerald-50 to-teal-50 rounded-bl-full opacity-60 -z-0" />
          <div className="z-10 flex items-center justify-between">
            <span className="text-sm font-bold text-slate-500">Tỉ lệ tuân thủ (7 ngày)</span>
            <div className="p-2 bg-emerald-50 text-emerald-600 rounded-xl border border-emerald-100">
              <Heart className="w-5 h-5 fill-emerald-100" />
            </div>
          </div>
          <div className="mt-4 z-10">
            <h2 className="text-3xl font-black text-slate-900">{kpi.overallAdherence}%</h2>
            <div className="mt-2 flex items-center gap-1.5">
              <span className={`inline-flex items-center gap-1 text-xs font-bold px-2 py-0.5 rounded-lg border ${trend.color}`}>
                {trend.icon}
                {trend.text}
              </span>
            </div>
          </div>
        </div>

        {/* KPI: Doses Today */}
        <div
          className="bg-white rounded-2xl border border-slate-150 p-5 shadow-sm shadow-slate-200/50 flex flex-col justify-between min-h-[140px] relative overflow-hidden transition-all duration-300 hover:-translate-y-1"
        >
          <div className="absolute top-0 right-0 w-24 h-24 bg-gradient-to-br from-sky-50 to-blue-50 rounded-bl-full opacity-60 -z-0" />
          <div className="z-10 flex items-center justify-between">
            <span className="text-sm font-bold text-slate-500">Liều thuốc hôm nay</span>
            <div className="p-2 bg-sky-50 text-sky-600 rounded-xl border border-sky-100">
              <Pill className="w-5 h-5 text-sky-600" />
            </div>
          </div>
          <div className="mt-4 z-10">
            <h2 className="text-3xl font-black text-slate-900">
              {kpi.takenDosesToday} <span className="text-lg font-bold text-slate-400">/ {kpi.totalDosesToday} liều</span>
            </h2>
            <div className="mt-2">
              <div className="w-full bg-slate-100 rounded-full h-1.5 overflow-hidden">
                <div
                  className="bg-sky-500 h-1.5 rounded-full transition-all duration-500"
                  style={{ width: `${kpi.totalDosesToday > 0 ? (kpi.takenDosesToday / kpi.totalDosesToday) * 100 : 0}%` }}
                />
              </div>
            </div>
          </div>
        </div>

        {/* KPI: Missed Today */}
        <div
          className="bg-white rounded-2xl border border-slate-150 p-5 shadow-sm shadow-slate-200/50 flex flex-col justify-between min-h-[140px] relative overflow-hidden transition-all duration-300 hover:-translate-y-1"
        >
          <div className="absolute top-0 right-0 w-24 h-24 bg-gradient-to-br from-rose-50 to-orange-50 rounded-bl-full opacity-60 -z-0" />
          <div className="z-10 flex items-center justify-between">
            <span className="text-sm font-bold text-slate-500">Liều bỏ lỡ / trễ</span>
            <div className="p-2 bg-rose-50 text-rose-600 rounded-xl border border-rose-100">
              <Clock className="w-5 h-5 text-rose-600" />
            </div>
          </div>
          <div className="mt-4 z-10">
            <h2 className={`text-3xl font-black ${kpi.missedDosesToday > 0 ? 'text-rose-600' : 'text-slate-900'}`}>
              {kpi.missedDosesToday}
            </h2>
            <p className="mt-1.5 text-xs font-semibold text-slate-500">
              {kpi.missedDosesToday > 0 ? 'Cần nhắc người thân uống ngay' : 'Mọi người đang uống đúng giờ'}
            </p>
          </div>
        </div>

        {/* KPI: Attention Required */}
        <div
          className="bg-white rounded-2xl border border-slate-150 p-5 shadow-sm shadow-slate-200/50 flex flex-col justify-between min-h-[140px] relative overflow-hidden transition-all duration-300 hover:-translate-y-1"
        >
          <div className="absolute top-0 right-0 w-24 h-24 bg-gradient-to-br from-amber-50 to-yellow-50 rounded-bl-full opacity-60 -z-0" />
          <div className="z-10 flex items-center justify-between">
            <span className="text-sm font-bold text-slate-500">Người thân cần chú ý</span>
            <div className="p-2 bg-amber-50 text-amber-600 rounded-xl border border-amber-100">
              <AlertTriangle className="w-5 h-5 text-amber-600" />
            </div>
          </div>
          <div className="mt-4 z-10">
            <h2 className="text-3xl font-black text-slate-900">
              {kpi.attentionCount} <span className="text-sm font-bold text-slate-400">người cần can thiệp</span>
            </h2>
            <div className="mt-2 flex items-center gap-1.5">
              {kpi.attentionCount > 0 ? (
                <span className="w-2.5 h-2.5 rounded-full bg-rose-500 animate-ping" />
              ) : null}
              <span className="text-xs font-bold text-slate-500">
                {kpi.attentionCount > 0 ? 'Phát hiện cảnh báo tuân thủ hoặc thuốc sắp hết' : 'Tất cả trạng thái đều an toàn'}
              </span>
            </div>
          </div>
        </div>
      </div>

      {/* Main Grid: Left Trend & Attention, Right Timeline & Warnings */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        
        {/* Column 1 & 2: Charts & Attention (Span 2) */}
        <div className="lg:col-span-2 space-y-6">
          
          {/* Chart Frame */}
          <div className="bg-white rounded-2xl border border-slate-150 p-5 shadow-sm shadow-slate-200/50">
            <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2 mb-4">
              <div>
                <p className="text-xs font-black uppercase text-slate-400 tracking-wider">Xu hướng 7 ngày</p>
                <h3 className="text-lg font-black text-slate-900">Mức độ tuân thủ thuốc</h3>
              </div>
              <div className="flex items-center gap-4 text-xs font-bold text-slate-500">
                <div className="flex items-center gap-1.5">
                  <span className="w-3 h-3 rounded bg-emerald-500" />
                  <span>Đạt chuẩn (&ge;80%)</span>
                </div>
              </div>
            </div>
            
            {chartData.length > 0 ? (
              <div className="h-64 mt-2">
                <ResponsiveContainer width="100%" height="100%">
                  <AreaChart data={chartData} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
                    <defs>
                      <linearGradient id="colorAdherence" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="5%" stopColor="#10b981" stopOpacity={0.3} />
                        <stop offset="95%" stopColor="#10b981" stopOpacity={0} />
                      </linearGradient>
                    </defs>
                    <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#f1f5f9" />
                    <XAxis
                      dataKey="dateFormatted"
                      tickLine={false}
                      axisLine={false}
                      tick={{ fill: '#64748b', fontSize: 11, fontWeight: 'bold' }}
                    />
                    <YAxis
                      domain={[0, 100]}
                      tickLine={false}
                      axisLine={false}
                      tick={{ fill: '#64748b', fontSize: 11, fontWeight: 'bold' }}
                    />
                    <Tooltip
                      contentStyle={{
                        backgroundColor: '#ffffff',
                        border: '1px solid #e2e8f0',
                        borderRadius: '12px',
                        boxShadow: '0 4px 6px -1px rgb(0 0 0 / 0.1)',
                      }}
                      labelStyle={{ fontWeight: 'black', color: '#1e293b' }}
                      formatter={(value, name, props) => [
                        `${value}%`,
                        `Tỉ lệ (Đã uống ${props.payload.taken}/${props.payload.total} liều)`,
                      ]}
                    />
                    <Area
                      type="monotone"
                      dataKey="Tỉ lệ"
                      stroke="#10b981"
                      strokeWidth={3}
                      fillOpacity={1}
                      fill="url(#colorAdherence)"
                    />
                  </AreaChart>
                </ResponsiveContainer>
              </div>
            ) : (
              <div className="h-64 flex items-center justify-center bg-slate-50 rounded-xl border border-dashed border-slate-200">
                <p className="text-slate-400 text-sm font-semibold">Chưa có lịch sử uống thuốc để hiển thị biểu đồ</p>
              </div>
            )}
            <div className="mt-4 p-3 bg-slate-50 rounded-xl border border-slate-100 text-slate-500 text-xs font-semibold flex items-start gap-2 leading-relaxed">
              <Info className="w-4 h-4 text-emerald-600 shrink-0 mt-0.5" />
              <span>
                Biểu đồ đo lường tỉ lệ uống thuốc đúng hẹn của tất cả người thân gộp lại theo từng ngày. Tỉ lệ tuân thủ được tính dựa trên số lượng liều đã xác nhận uống (TAKEN) chia cho tổng số liều được lập lịch (không bao gồm liều PENDING chưa đến giờ).
              </span>
            </div>
          </div>

          {/* Attention Table */}
          <div className="bg-white rounded-2xl border border-slate-150 p-5 shadow-sm shadow-slate-200/50">
            <div>
              <p className="text-xs font-black uppercase text-slate-400 tracking-wider">Cần can thiệp khẩn</p>
              <h3 className="text-lg font-black text-slate-900">Danh sách người thân cần chú ý</h3>
            </div>
            
            {attentionList.length > 0 ? (
              <div className="mt-4 grid gap-3">
                {attentionList.map((person) => {
                  const isHigh = person.severity === 'HIGH'
                  return (
                    <div
                      key={person.patientId}
                      className={`flex flex-col sm:flex-row sm:items-center justify-between gap-4 p-4 rounded-xl border transition-all duration-300 hover:scale-[1.01] ${
                        isHigh
                          ? 'bg-rose-50/50 border-rose-100 hover:bg-rose-50'
                          : 'bg-amber-50/40 border-amber-100 hover:bg-amber-50/70'
                      }`}
                    >
                      <div className="flex items-center gap-3">
                        {/* Avatar representation */}
                        <div className="w-11 h-11 rounded-xl bg-gradient-to-tr from-slate-100 to-slate-200 border border-slate-300 flex items-center justify-center font-bold text-slate-700 overflow-hidden shrink-0">
                          {person.avatarUrl ? (
                            <img src={person.avatarUrl} alt={person.patientName} className="w-full h-full object-cover" />
                          ) : (
                            <User className="w-5 h-5 text-slate-500" />
                          )}
                        </div>
                        <div>
                          <div className="flex items-center gap-2">
                            <span className="font-black text-slate-900 text-sm">{person.patientName}</span>
                            <span className="text-xs font-bold px-2 py-0.5 rounded bg-slate-100 border border-slate-200 text-slate-600">
                              {person.relationLabel}
                            </span>
                          </div>
                          <p className="text-xs font-semibold text-slate-500 mt-1 flex items-center gap-1">
                            <span className={`w-1.5 h-1.5 rounded-full ${isHigh ? 'bg-rose-500 animate-ping' : 'bg-amber-500'}`} />
                            <strong className={isHigh ? 'text-rose-700' : 'text-amber-800'}>
                              {person.reason}
                            </strong>
                          </p>
                        </div>
                      </div>
                      <div className="flex items-center gap-2 self-end sm:self-center">
                        <span className={`text-xs font-black px-2.5 py-1 rounded-lg border ${
                          isHigh
                            ? 'bg-rose-100 text-rose-800 border-rose-200'
                            : 'bg-amber-100 text-amber-800 border-amber-200'
                        }`}>
                          {isHigh ? 'Cấp bách' : 'Trung bình'}
                        </span>
                      </div>
                    </div>
                  )
                })}
              </div>
            ) : (
              <div className="mt-4 p-8 flex flex-col items-center justify-center bg-slate-50 border border-dashed border-slate-200 rounded-xl text-center">
                <CheckCircle2 className="w-10 h-10 text-emerald-600" />
                <h4 className="mt-3 font-black text-slate-800 text-sm">Tuyệt vời! Không phát hiện vấn đề</h4>
                <p className="text-xs text-slate-500 mt-1 max-w-sm font-semibold">
                  Tất cả người thân đều đang tuân thủ lịch trình tốt và các hộp thuốc trong tủ đều còn đầy đủ số lượng.
                </p>
              </div>
            )}
          </div>
        </div>

        {/* Column 3: Timeline & Recent Warnings */}
        <div className="space-y-6">
          
          {/* Today Doses Timeline */}
          <div className="bg-white rounded-2xl border border-slate-150 p-5 shadow-sm shadow-slate-200/50 flex flex-col">
            <div>
              <p className="text-xs font-black uppercase text-slate-400 tracking-wider">Hôm nay</p>
              <h3 className="text-lg font-black text-slate-900">Lịch trình uống thuốc</h3>
            </div>
            
            {timeline.length > 0 ? (
              <div className="mt-5 space-y-4 max-h-[460px] overflow-y-auto pr-1">
                {timeline.map((dose, idx) => {
                  const badge = getStatusBadge(dose.status)
                  const timeStr = new Date(dose.scheduledAt).toLocaleTimeString('vi-VN', {
                    hour: '2-digit',
                    minute: '2-digit',
                  })
                  
                  return (
                    <div key={dose.eventId} className="flex gap-3 items-start relative group">
                      {/* Vertical line indicator */}
                      {idx !== timeline.length - 1 && (
                        <div className="absolute left-[18px] top-9 bottom-0 w-0.5 bg-slate-100 group-hover:bg-slate-200 transition-colors" />
                      )}
                      
                      {/* Left circular node indicating state */}
                      <div className={`w-9 h-9 rounded-xl border flex items-center justify-center font-bold text-xs shrink-0 z-10 transition-transform group-hover:scale-105 shadow-sm ${
                        dose.status === 'TAKEN' ? 'bg-emerald-50 border-emerald-300 text-emerald-600' :
                        dose.status === 'MISSED' ? 'bg-rose-50 border-rose-300 text-rose-600 animate-pulse' :
                        dose.status === 'OVERDUE' ? 'bg-amber-50 border-amber-300 text-amber-600' :
                        'bg-slate-50 border-slate-200 text-slate-500'
                      }`}>
                        {timeStr}
                      </div>

                      {/* Main card info */}
                      <div className="flex-1 min-w-0 bg-slate-50/50 hover:bg-slate-50 border border-slate-100 rounded-xl p-3 transition-all">
                        <div className="flex items-center justify-between gap-2">
                          <strong className="text-xs font-black text-slate-900 truncate">
                            {dose.patientName}
                          </strong>
                          <span className="text-[10px] text-slate-400 font-bold">
                            ({dose.relationLabel})
                          </span>
                        </div>
                        <div className="mt-1 flex items-center justify-between gap-2">
                          <p className="text-xs font-semibold text-slate-600 truncate">
                            {dose.pillName} ({dose.dosageAmount} {dose.dosageUnit})
                          </p>
                          <span className={`inline-flex items-center gap-1 text-[10px] font-black px-1.5 py-0.5 rounded border leading-none ${badge.bg}`}>
                            {badge.icon}
                            {badge.label}
                          </span>
                        </div>
                      </div>
                    </div>
                  )
                })}
              </div>
            ) : (
              <div className="mt-4 p-8 flex flex-col items-center justify-center bg-slate-50 border border-dashed border-slate-200 rounded-xl text-center">
                <p className="text-slate-400 text-xs font-semibold">Chưa lập lịch trình dùng thuốc trong ngày hôm nay.</p>
              </div>
            )}
          </div>

          {/* Recent Alerts (24h) */}
          <div className="bg-white rounded-2xl border border-slate-150 p-5 shadow-sm shadow-slate-200/50 flex flex-col">
            <div>
              <p className="text-xs font-black uppercase text-slate-400 tracking-wider">Lịch sử cảnh báo</p>
              <h3 className="text-lg font-black text-slate-900 flex items-center gap-1.5">
                Cảnh báo gần đây (24h)
                {warnings.length > 0 && (
                  <span className="relative flex h-2 w-2">
                    <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-rose-400 opacity-75"></span>
                    <span className="relative inline-flex rounded-full h-2 w-2 bg-rose-500"></span>
                  </span>
                )}
              </h3>
            </div>

            {warnings.length > 0 ? (
              <div className="mt-4 space-y-3">
                {warnings.slice(0, 5).map((warn) => {
                  const alertTime = new Date(warn.scheduledAt).toLocaleTimeString('vi-VN', {
                    hour: '2-digit',
                    minute: '2-digit',
                  })
                  const alertDate = new Date(warn.scheduledAt).toLocaleDateString('vi-VN', {
                    day: 'numeric',
                    month: 'numeric',
                  })
                  return (
                    <div
                      key={warn.id}
                      className="flex items-start gap-2.5 p-3 rounded-xl border border-rose-100 bg-rose-50/20 hover:bg-rose-50/40 transition-colors"
                    >
                      <ShieldAlert className="w-5 h-5 text-rose-500 shrink-0 mt-0.5" />
                      <div className="min-w-0 flex-1">
                        <p className="text-xs font-semibold text-slate-800 leading-normal">
                          <strong className="font-bold text-rose-900">{warn.patientName}</strong> ({warn.relationLabel}) đã bỏ lỡ liều <strong className="font-bold text-rose-900">{warn.pillName}</strong>.
                        </p>
                        <span className="text-[10px] font-bold text-slate-400 block mt-1">
                          Lập lịch: {alertTime} ngày {alertDate} · Trạng thái: {warn.status === 'MISSED' ? 'Bỏ lỡ' : 'Trễ giờ'}
                        </span>
                      </div>
                    </div>
                  )
                })}
              </div>
            ) : (
              <div className="mt-4 p-8 flex flex-col items-center justify-center bg-slate-50 border border-dashed border-slate-200 rounded-xl text-center">
                <CheckCircle2 className="w-8 h-8 text-emerald-600" />
                <h4 className="mt-2 font-bold text-slate-800 text-xs">Mọi người đều an toàn</h4>
                <p className="text-[10px] text-slate-400 mt-1 max-w-[200px] font-semibold">
                  Không ghi nhận trường hợp bỏ lỡ thuốc nào trong vòng 24 giờ qua.
                </p>
              </div>
            )}
          </div>
          
        </div>

      </div>

    </div>
  )
}
